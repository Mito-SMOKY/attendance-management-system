package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

import lombok.RequiredArgsConstructor;

// 授業終了時に、全員分の出席・欠席を確定させるサービス
@Service
@RequiredArgsConstructor
public class SessionAttendanceService {

    private final EnrollmentsRepository enrollmentsRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceStatusRepository attendanceStatusRepository;
    private final EntryLogService entryLogService;

    // 出席確定処理
    @Transactional
    public void finalizeAttendance(SessionEntity session, Map<Integer, Integer> manualChanges, LocalDateTime now) {
        
        // 生徒リストを取得
        Integer targetGrade = session.getTargetGrade() != null ? session.getTargetGrade() : 1;
        List<EnrollmentsEntity> enrollments = enrollmentsRepository.findByDepartmentIdAndGrade(
            session.getTargetDepartmentId(), 
            targetGrade
        );

        // タッチログを取得
        List<EntryLogEntity> logs = entryLogService.findLogsForSession(
            session.getActualClassroomId(), 
            session.getStartTime().minusMinutes(15), 
            now
        );

        // 生徒一人ひとりについて判定と保存を行う
        for (EnrollmentsEntity enrollment : enrollments) {
            Integer userId = enrollment.getStudent().getUserId();

            // 既にデータがあれば取得、なければ新規作成する
            AttendanceEntity attendance = attendanceRepository.findBySessionIdAndStudent_UserId(session.getSessionId(), userId)
                    .orElse(new AttendanceEntity());

            // 新規作成の場合の初期設定
            if (attendance.getAttendanceId() == null) {
                attendance.setSessionId(session.getSessionId());
                attendance.setTimeTable(session.getTimeTable());
                attendance.setStudent(enrollment.getStudent());
                attendance.setCreatedAt(now);
            }

            // 生徒のログの有無の確認
            boolean hasLog = logs.stream().anyMatch(l -> l.getUserId().equals(userId));
            int statusId = hasLog ? 1 : 2; // ログあり=出席(1), なし=欠席(2)

            // 手動判定の優先
            if (manualChanges != null && manualChanges.containsKey(userId)) {
                statusId = manualChanges.get(userId);
            }

            // ステータスIDをEntityに変換してセット
            AttendanceStatusEntity statusEntity = attendanceStatusRepository.findById(statusId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Status ID"));
            attendance.setStatusId(statusEntity);
            
            // データベースに保存
            attendanceRepository.save(attendance);
        }
    }
}