package com.example.attendancemanagementsystem.attendance.record.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.record.dto.RecordDTO;
import com.example.attendancemanagementsystem.attendance.record.dto.VerifiedRecordDto; 
import com.example.attendancemanagementsystem.common.entity.CardsEntity;
import com.example.attendancemanagementsystem.common.entity.ClassroomEntity;
import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.repository.CardsRepository;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.EntryLogRepository;

@Service
public class RecordService {

    private final CardsRepository cardsRepository; 
    private final DecryptionService decryptionService;
    private final EntryLogRepository entryLogRepository;
    private final ClassroomRepository classroomRepository;

    public RecordService(CardsRepository cardsRepository,
                        DecryptionService decryptionService,
                        EntryLogRepository entryLogRepository,
                        ClassroomRepository classroomRepository) {
        this.cardsRepository = cardsRepository;
        this.decryptionService = decryptionService;
        this.entryLogRepository = entryLogRepository;
        this.classroomRepository = classroomRepository;
    }

     // NFCスキャンデータを受け取り、チェックを行った上で入室ログを保存する
    @Transactional
    public VerifiedRecordDto processAttendanceScan(RecordDTO recordDTO) {
        // タイムスタンプの取得
        LocalDateTime scanTime;
        try {
            if (recordDTO.getReadTime() != null && !recordDTO.getReadTime().isEmpty()) {
                scanTime = LocalDateTime.parse(recordDTO.getReadTime());
            } else {
                scanTime = LocalDateTime.now();
            }
        } catch (Exception e) {
            scanTime = LocalDateTime.now();
        }

        // カードIDの存在チェック 
        String cardId = recordDTO.getCardId();
        if (cardId == null || cardId.isEmpty()) {
            throw new IllegalArgumentException("Card ID is empty");
        }
        
        //  新規カード登録へ
        Optional<CardsEntity> cardOpt = cardsRepository.findByCardId(cardId);
        if (cardOpt.isEmpty()) {
            throw new IllegalArgumentException("未登録のカードです: " + cardId);
        }

        // リーダー(教室)の特定とセキュリティチェック
        String readerMacAddress = recordDTO.getReaderId();
        Optional<ClassroomEntity> classroomOpt = classroomRepository.findByMacAddress(readerMacAddress);
        if (classroomOpt.isEmpty()) {
            throw new SecurityException("未登録のリーダー(MAC: " + readerMacAddress + ")からのアクセスは拒否されました。");
        }
        
        // ユーザーIDの復号
        String encryptedUserId = recordDTO.getUserId();
        String originalUserIdStr = decryptionService.decryptUserId(encryptedUserId);
        Integer userId = Integer.parseInt(originalUserIdStr);

        // EntryLogEntityの保存
        EntryLogEntity entryLog = new EntryLogEntity();
        entryLog.setUserId(userId);
        entryLog.setClassroomId(classroomOpt.get().getClassroomId());
        entryLog.setEntryTime(scanTime);
        entryLog.setIsProcessed(0); // 未処理

        entryLogRepository.save(entryLog);

        // 結果返却
        return new VerifiedRecordDto(
                userId,
                cardId,
                scanTime,
                readerMacAddress
        );
    }
}