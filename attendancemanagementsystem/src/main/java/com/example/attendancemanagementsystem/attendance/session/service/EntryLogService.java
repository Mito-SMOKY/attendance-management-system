package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.repository.EntryLogRepository;


@Service
public class EntryLogService {

    private final EntryLogRepository entryLogRepository;

    public EntryLogService(EntryLogRepository entryLogRepository) {
        this.entryLogRepository = entryLogRepository;
    }

    // セッション終了時に関連する入室ログを取得するメソッド
    public List<EntryLogEntity> getLogsForEndSession(SessionEntity session, LocalDateTime now) {
        
        // セッションの開始時刻を取得
        LocalDateTime actualStartTime = session.getStartTime(); 

        // 検索開始は「セッション開始時刻の1時間前」
        LocalDateTime searchStart = actualStartTime.minusMinutes(60);
        
        // 検索終了は「現在時刻」
        LocalDateTime searchEnd = now;

        // 指定した教室IDと時間範囲で、未処理のログを取得
        return entryLogRepository.findByClassroomIdAndEntryTimeBetween(
            session.getClassroom().getClassroomId(), 
            searchStart, 
            searchEnd
        );
    }

    // 取得した入室ログを処理済みに更新するメソッド
    @Transactional
    public void markLogsAsProcessed(List<EntryLogEntity> logs, LocalDateTime processedAt) {

        // ログが空でない場合にのみ処理
        if (logs.isEmpty()) return;

        // 各ログの処理済みフラグと処理日時を更新
        for (EntryLogEntity log : logs) {
            if (log.getIsProcessed() == null || log.getIsProcessed() == 0) {
                log.setIsProcessed(1);
                log.setProcessedAt(processedAt);
            }
        }
        entryLogRepository.saveAll(logs);
    }
}