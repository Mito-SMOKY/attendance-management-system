package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.AttendanceStatusEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;

@Service
public class SessionAttendanceService {

    private final EnrollmentsRepository enrollmentsRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceStatusRepository attendanceStatusRepository;
    private final SessionRepository sessionRepository;

    public SessionAttendanceService(EnrollmentsRepository enrollmentsRepository,
                                    AttendanceRepository attendanceRepository,
                                    AttendanceStatusRepository attendanceStatusRepository,
                                    SessionRepository sessionRepository) {
        this.enrollmentsRepository = enrollmentsRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public void registerAttendance(SessionEntity session, List<EntryLogEntity> logs, Map<Integer, Integer> manualChanges) {
        
        // ステータスマスタをMap化
        Map<Integer, AttendanceStatusEntity> statusMap = attendanceStatusRepository.findAll().stream()
                .collect(Collectors.toMap(AttendanceStatusEntity::getStatusId, Function.identity()));
        
        // 履修生を取得 (DepartmentIdとGradeで絞り込み)
        List<EnrollmentsEntity> enrollments = enrollmentsRepository.findByDepartmentIdAndGrade(
            session.getDepartment().getDepartmentId(), 
            session.getTargetGrade()
        );

        // 判定基準時間
        LocalDateTime baseTime = session.getStartTime();
        LocalDateTime lateBoundary = baseTime.plusMinutes(20);   // 20分まで
        LocalDateTime absentBoundary = baseTime.plusMinutes(30); // 30分まで

        // ★追加ロジック: 直前の授業（同教室）の出席状況を取得
        Map<Integer, Integer> prevStatusMap = getPreviousSessionStatusMap(session);

        List<AttendanceEntity> attendancesToSave = new ArrayList<>();

        for (EnrollmentsEntity enrollment : enrollments) {
            Integer studentId = enrollment.getStudent().getUserId();
            AttendanceEntity attendance = new AttendanceEntity();
            attendance.setSessionId(session.getSessionId());
            attendance.setStudent(enrollment.getStudent());

            Integer statusId;

            // 1. 手動変更がある場合を最優先
            if (manualChanges != null && manualChanges.containsKey(studentId)) {
                statusId = manualChanges.get(studentId);
            } 
            else {
                // 2. 入室ログ判定
                EntryLogEntity myLog = logs.stream()
                    .filter(l -> l.getUserId().equals(studentId))
                    .findFirst().orElse(null);

                if (myLog != null) {
                    if (myLog.getEntryTime().isAfter(absentBoundary)) {
                        statusId = 2; // 欠席
                    } else if (myLog.getEntryTime().isAfter(lateBoundary)) {
                        statusId = 3; // 遅刻
                    } else {
                        statusId = 1; // 出席
                    }
                } else {
                    // 3. 連続授業判定 (ログがない場合)
                    // 直前の授業（同教室）があれば、そのステータスを引き継ぐ
                    if (prevStatusMap.containsKey(studentId)) {
                        Integer prevStatus = prevStatusMap.get(studentId);
                        if (prevStatus == 2) {
                            statusId = 2; // 前回欠席 -> 今回も欠席
                        } else if (prevStatus == 6) {
                            statusId = 6; // 前回出席停止 -> 今回も停止
                        } else {
                            statusId = 1; // 前回出席/遅刻 -> 今回は出席(連続)
                        }
                    } else {
                        statusId = 2; // 前回のデータもない/教室移動あり -> 欠席
                    }
                }
            }

            // Entity設定
            AttendanceStatusEntity statusEntity = statusMap.get(statusId);
            if (statusEntity == null) statusEntity = statusMap.get(2); // デフォルト欠席
            
            attendance.setStatusId(statusEntity);
            attendancesToSave.add(attendance);
        }

        // 一括保存
        attendanceRepository.saveAll(attendancesToSave);
    }

    /**
     * 同じ日・同じ教科・同じ学科・同じ学年の「一つ前のコマ」の出席状況を取得するヘルパーメソッド
     * ※「同じ教室」である場合のみデータを返す
     */
    private Map<Integer, Integer> getPreviousSessionStatusMap(SessionEntity currentSession) {
        Map<Integer, Integer> map = new HashMap<>();

        // 1. 同日の同条件（学科・学年・科目・日付）のセッションを検索
        List<SessionEntity> sameDaySessions = sessionRepository.findByDepartmentAndTargetGradeAndSubjectAndSessionDate(
                currentSession.getDepartment(),
                currentSession.getTargetGrade(),
                currentSession.getSubject(),
                currentSession.getSessionDate()
        );

        // 2. 現在のコマ(SlotId)より小さい中で最大のものを探す（＝直前の授業）
        Optional<SessionEntity> prevSessionOpt = sameDaySessions.stream()
                .filter(s -> s.getTimeSlot().getSlotId() < currentSession.getTimeSlot().getSlotId())
                .max(Comparator.comparingInt(s -> s.getTimeSlot().getSlotId()));

        // 3. 直前の授業があり、かつ「同じ教室」である場合のみ引き継ぎ対象とする
        if (prevSessionOpt.isPresent()) {
            SessionEntity prevSession = prevSessionOpt.get();

            // 教室IDの比較
            boolean isSameClassroom = prevSession.getClassroom().getClassroomId()
                                        .equals(currentSession.getClassroom().getClassroomId());

            if (isSameClassroom) {
                List<AttendanceEntity> prevAttendances = attendanceRepository.findBySessionId(prevSession.getSessionId());
                for (AttendanceEntity att : prevAttendances) {
                    map.put(att.getStudent().getUserId(), att.getStatus().getStatusId());
                }
            }
        }

        return map;
    }
}