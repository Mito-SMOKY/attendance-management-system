package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.repository.EntryLogRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor

// カードリーダーのログ操作
public class EntryLogService {

    private final EntryLogRepository entryLogRepository;

    // 画面表示用にログを検索する
    public List<EntryLogEntity> findLogsForSession(Integer classroomId, LocalDateTime startTime, LocalDateTime endTime) {
        return entryLogRepository.findByClassroomIdAndEntryTimeBetween(classroomId, startTime, endTime);
    }

    // 授業終了後にログを処理済みに更新する
    @Transactional
    public void processRemainingLogs(SessionEntity session, LocalDateTime now) {
        LocalDateTime searchLimit = session.getStartTime().plusMinutes(85);
        LocalDateTime searchEnd = now.isBefore(searchLimit) ? now : searchLimit;
        
        //授業開始15分前から終了までのログを取得
        List<EntryLogEntity> logs = entryLogRepository.findByClassroomIdAndEntryTimeBetween(
            session.getActualClassroomId(), 
            session.getStartTime().minusMinutes(15), 
            searchEnd
        );
        
        //ログの更新
        for (EntryLogEntity log : logs) {
            if (log.getIsProcessed() == null || log.getIsProcessed() == 0) {
                log.setIsProcessed(1);
                log.setProcessedAt(now);
                entryLogRepository.save(log);
            }
        }
    }
}