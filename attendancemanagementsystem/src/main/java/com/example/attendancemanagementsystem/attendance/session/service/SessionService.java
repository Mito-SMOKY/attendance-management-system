package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private final SessionRepository sessionRepository;
    private final EntryLogRepository entryLogRepository;
    private final AttendanceRepository attendanceRepository;
    
    // IDからエンティティを検索するために追加したリポジトリ
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
     * 授業を終了し、ログから出席データを生成して保存する
     */
    @Transactional
    public void endSession(Integer sessionId) {
        // 1. セッション終了処理
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        LocalDateTime now = LocalDateTime.now();
        session.setEndTime(now);
        session.setSessionStatus(0); // 終了ステータス
        sessionRepository.save(session);

        // 遅刻ライン算出 (開始時間 + 20分)
        int lateLimitMinutes = 20; 
        LocalDateTime lateBoundary = session.getStartTime().plusMinutes(lateLimitMinutes);

        // 2. ログ取得 (開始30分前〜現在)
        LocalDateTime searchStart = session.getStartTime().minusMinutes(30);
        List<EntryLogEntity> logs = entryLogRepository.findByClassroomIdAndEntryTimeBetween(
                session.getActualClassroomId(), searchStart, now
        );

        // 重複処理防止用セット
        Set<Integer> processedUserIds = new HashSet<>();

        for (EntryLogEntity log : logs) {
            Integer userId = log.getUserId();
            // すでに処理済みのユーザーならスキップ
            if (processedUserIds.contains(userId)) continue;

            // ★ID(数字)を使って、DBから「学生オブジェクト」を取得
            StudentEntity student = studentRepository.findById(userId).orElse(null);
            
            // 学生マスタにいないIDの場合はスキップ
            if (student == null) continue;

            // ステータスIDの決定（遅刻判定: 2=遅刻, 1=出席）
            int statusId = log.getEntryTime().isAfter(lateBoundary) ? 3 : 1;
            
            // ★ID(数字)を使って、DBから「ステータスオブジェクト」を取得
            AttendanceStatusEntity status = attendanceStatusRepository.findById(statusId).orElse(null);
            
            // ステータスマスタにデータがないとエラーになるためチェック（必要ならエラーログ等を出す）
            if (status == null) {
                System.out.println("Error: StatusID " + statusId + " not found in DB.");
                continue; 
            }

            // --- ご希望の書き方でエンティティを作成 ---
            AttendanceEntity attendance = new AttendanceEntity();
            
            attendance.setStudent(student);         // 学生エンティティをセット
            attendance.setStatusId(status);         // ステータスエンティティをセット
            attendance.setSessionId(sessionId);     // セッションIDをセット
            attendance.setCreatedAt(LocalDateTime.now());
            attendance.setTimeTable(null);          // 時間割は今回使わないのでnull
            
            // DBへ保存 (INSERT)
            attendanceRepository.save(attendance);
            // ----------------------------------

            processedUserIds.add(userId);
        }
    }
}