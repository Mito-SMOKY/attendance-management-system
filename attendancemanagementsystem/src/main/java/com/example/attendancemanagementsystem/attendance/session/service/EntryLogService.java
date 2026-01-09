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

    /**
     * 授業終了時、出席判定用のログを取得する
     */
    public List<EntryLogEntity> getLogsForEndSession(SessionEntity session, LocalDateTime now) {
        
        // 先生が「開始ボタン」を押した時間（実測）
        LocalDateTime actualStartTime = session.getStartTime(); 

        // 探す開始時間は「開始時刻の60分前」
        LocalDateTime searchStart = actualStartTime.minusMinutes(60);
        
        // 終わりは「現在時刻（終了ボタンを押した時間）」
        LocalDateTime searchEnd = now;

        // 「未処理(isProcessed=False)」かつ「この部屋」のログだけを拾う
        return entryLogRepository.findByClassroomIdAndEntryTimeBetween(
            session.getClassroom().getClassroomId(), 
            searchStart, 
            searchEnd
        );
    }

    /**
     * ログを処理済みにする（変更なし）
     */
    @Transactional
    public void markLogsAsProcessed(List<EntryLogEntity> logs, LocalDateTime processedAt) {
        if (logs.isEmpty()) return;
        for (EntryLogEntity log : logs) {
            if (log.getIsProcessed() == null || log.getIsProcessed() == 0) {
                log.setIsProcessed(1);
                log.setProcessedAt(processedAt);
            }
        }
        entryLogRepository.saveAll(logs);
    }
}