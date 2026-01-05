package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final SessionRepository SessionRepository;
    private final EntryLogRepository entryLogRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceStatusRepository attendanceStatusRepository;
    private final StudentRepository studentRepository;

    public SessionService(SessionRepository SessionRepository,
                        EntryLogRepository entryLogRepository,
                        AttendanceRepository attendanceRepository,
                        AttendanceStatusRepository attendanceStatusRepository,
                        StudentRepository studentRepository) {
        this.SessionRepository = SessionRepository;
        this.entryLogRepository = entryLogRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * セッションを終了し、入室ログを出席データとして確定させる
     */
    @Transactional
    public void endSession(Integer sessionId) {
        // 1. セッション情報の取得
        SessionEntity session = SessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        LocalDateTime now = LocalDateTime.now();

        // 終了時刻を更新
        session.setEndTime(now); 
        // ステータスを「終了(例:0)」に変更
        session.setSessionStatus(0); 
        
        SessionRepository.save(session);

        // 2. 対象の入室ログを取得
        // Entityの定義に合わせて "actualClassroomId" を使用
        // ※開始時間の30分前から、今の終了時間までのログを拾う設定です
        LocalDateTime searchStart = session.getStartTime().minusMinutes(30);
        
        List<EntryLogEntity> logs = entryLogRepository.findByClassroomIdAndEntryTimeBetween(
                session.getActualClassroomId(),
                searchStart,
                now
        );

        // 3. ログを集計してAttendanceに変換
        for (EntryLogEntity log : logs) {
            // 処理済みならスキップ
            if (log.getIsProcessed() != null && log.getIsProcessed() == 1) {
                continue;
            }

            StudentEntity student = studentRepository.findById(log.getUserId())
                    .orElse(null);
            
            if (student == null) continue;

            // 出席レコード作成
            AttendanceEntity attendance = new AttendanceEntity();
            attendance.setStudent(student);
            attendance.setSessionId(sessionId);
            attendance.setCreatedAt(LocalDateTime.now());

            // 遅刻判定
            int statusId = determineStatus(session.getStartTime(), log.getEntryTime());
            
            AttendanceStatusEntity status = attendanceStatusRepository.findById(statusId)
                    .orElseThrow(() -> new IllegalStateException("Status ID not found: " + statusId));
            attendance.setStatusId(status);

            attendanceRepository.save(attendance);

            // ログを処理済みに更新
            log.setIsProcessed(1);
            log.setProcessedAt(now);
            entryLogRepository.save(log);
        }
    }

    /**
     * 遅刻判定ロジック
     */
    private int determineStatus(LocalDateTime startTime, LocalDateTime entryTime) {
        // 例: 開始15分後までなら出席(1)、それ以降は遅刻(2)
        LocalDateTime lateThreshold = startTime.plusMinutes(15);

        if (entryTime.isBefore(lateThreshold)) {
            return 1; // 出席
        } else {
            return 2; // 遅刻
        }
    }
}