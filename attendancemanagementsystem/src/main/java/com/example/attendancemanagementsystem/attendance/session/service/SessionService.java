package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.session.dto.SessionDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.AttendanceStatusEntity;
import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository;
import com.example.attendancemanagementsystem.common.repository.EntryLogRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final EntryLogRepository entryLogRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final AttendanceStatusRepository attendanceStatusRepository;

    public SessionService(SessionRepository sessionRepository,
                          EntryLogRepository entryLogRepository,
                          AttendanceRepository attendanceRepository,
                          StudentRepository studentRepository,
                          AttendanceStatusRepository attendanceStatusRepository) {
        this.sessionRepository = sessionRepository;
        this.entryLogRepository = entryLogRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
    }

    /**
     * 画面表示用：出席状況の取得
     */
    public List<SessionDto> getSessionAttendees(Integer sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // 学年・年度の計算
        int currentYear = LocalDate.now().getYear();
        if (LocalDate.now().getMonthValue() < 4) currentYear--;
        Integer targetGrade = session.getTargetGrade() != null ? session.getTargetGrade() : 1;

        // 学生リスト取得
        List<StudentEntity> allStudents = studentRepository.findByDepartmentAndGrade(
                session.getTargetDepartmentId(), targetGrade, currentYear
        );

        // ログ取得：授業開始の15分前から検索（休憩時間のタッチも拾う）
        LocalDateTime searchStart = session.getStartTime().minusMinutes(15);
        List<EntryLogEntity> logs = entryLogRepository.findByClassroomIdAndEntryTimeBetween(
                session.getActualClassroomId(), searchStart, LocalDateTime.now()
        );

        // 既存の出席データ取得
        List<AttendanceEntity> existingAttendances = attendanceRepository.findBySessionId(sessionId);
        Map<Integer, AttendanceEntity> attendanceMap = existingAttendances.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getUserId(), a -> a));

        List<SessionDto> result = new ArrayList<>();
        
        // ★固定設定：遅刻は開始20分後から
        LocalDateTime lateBoundary = session.getStartTime().plusMinutes(20);

        for (StudentEntity student : allStudents) {
            SessionDto dto = new SessionDto();
            Integer userId = student.getUserId();
            dto.setUserId(userId);
            String name = (student.getUsers() != null) ? student.getUsers().getName() : "Unknown";
            dto.setStudentName(name);
            dto.setGradeClass(targetGrade + "年");

            // 1. 既に保存済みのデータがある場合
            if (attendanceMap.containsKey(userId)) {
                AttendanceEntity saved = attendanceMap.get(userId);
                dto.setStatusId(saved.getStatus().getStatusId());
                dto.setEntryTime("--:--");
                logs.stream().filter(l -> l.getUserId().equals(userId)).findFirst()
                    .ifPresent(l -> dto.setEntryTime(l.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm"))));
            } 
            // 2. まだ保存されていない場合（リアルタイム判定）
            else {
                EntryLogEntity myLog = logs.stream()
                        .filter(l -> l.getUserId().equals(userId)).findFirst().orElse(null);

                if (myLog != null) {
                    // ログあり：遅刻判定
                    dto.setEntryTime(myLog.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                    dto.setStatusId(myLog.getEntryTime().isAfter(lateBoundary) ? 3 : 1);
                } else {
                    // ログなし：連続受講チェック！
                    // 「今の授業開始時刻」の直前（30分以内）に終わった、同じ教室の授業を探す
                    Optional<AttendanceEntity> prev = attendanceRepository.findPreviousAttendanceInSameRoom(
                            userId,
                            session.getActualClassroomId(),
                            session.getStartTime().minusMinutes(30), 
                            session.getStartTime()
                    );

                    if (prev.isPresent()) {
                        dto.setEntryTime("(連続)"); 
                        dto.setStatusId(1); // 出席扱い
                    } else {
                        dto.setEntryTime("--:--");
                        dto.setStatusId(2); // 欠席
                    }
                }
            }
            result.add(dto);
        }
        return result;
    }


    /**
     * 授業終了処理 (一括保存)
     */
    @Transactional
    public void endSession(Integer sessionId, Map<Integer, Integer> manualChanges) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // 1. セッション終了
        LocalDateTime now = LocalDateTime.now();
        session.setEndTime(now);
        session.setSessionStatus(0);
        sessionRepository.save(session);

        // 2. 出席データの確定保存
        List<SessionDto> finalStates = getSessionAttendees(sessionId); // 上記の判定ロジックを再利用

        for (SessionDto dto : finalStates) {
            AttendanceEntity attendance = attendanceRepository.findBySessionIdAndStudent_UserId(sessionId, dto.getUserId())
                    .orElse(new AttendanceEntity());

            if (attendance.getAttendanceId() == null) {
                attendance.setSessionId(sessionId);
                StudentEntity student = studentRepository.findById(dto.getUserId()).orElse(null);
                if (student == null) continue;
                attendance.setStudent(student);
                attendance.setCreatedAt(now);
            }

            // ステータス決定（手動変更があれば優先）
            Integer finalStatusId = dto.getStatusId();
            if (manualChanges != null && manualChanges.containsKey(dto.getUserId())) {
                finalStatusId = manualChanges.get(dto.getUserId());
            }

            AttendanceStatusEntity status = attendanceStatusRepository.findById(finalStatusId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Status ID"));
            attendance.setStatusId(status);

            attendanceRepository.save(attendance);
        }

        // 3. ログのフラグ処理（次の授業のログを食わないようにリミット設定）
        
        // ★固定設定：授業80分 + 予備5分 = 85分までしか見ない
        LocalDateTime searchLimit = session.getStartTime().plusMinutes(85);
        // 現在時刻とリミット時刻、早い方を採用
        LocalDateTime searchEnd = now.isBefore(searchLimit) ? now : searchLimit;

        List<EntryLogEntity> logs = entryLogRepository.findByClassroomIdAndEntryTimeBetween(
            session.getActualClassroomId(),
            session.getStartTime().minusMinutes(15), // 開始15分前から
            searchEnd                                // リミット付き終了時刻
        );

        for (EntryLogEntity log : logs) {
            if (log.getIsProcessed() == null || log.getIsProcessed() == 0) {
                log.setIsProcessed(1);
                log.setProcessedAt(now);
                entryLogRepository.save(log);
            }
        }
    }
}