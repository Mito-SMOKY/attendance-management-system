package com.example.attendancemanagementsystem.attendance.record.service;

import com.example.attendancemanagementsystem.attendance.record.dto.RecordDTO;
import com.example.attendancemanagementsystem.attendance.record.dto.VerifiedRecordDto;
import com.example.attendancemanagementsystem.common.entity.CardsEntity;
import com.example.attendancemanagementsystem.common.repository.CardsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RecordService {

    private final CardsRepository cardsRepository;
    private final DecryptionService decryptionService;

    // コンストラクタ
    public RecordService(CardsRepository cardsRepository, DecryptionService decryptionService) {
        this.cardsRepository = cardsRepository;
        this.decryptionService = decryptionService;
    }

    // 出欠記録スキャンデータの処理メソッド
    public VerifiedRecordDto processAttendanceScan(RecordDTO recordDTO) {

        //復号処理
        String encryptedHex = recordDTO.getUserId();
        String originalUserIdString = decryptionService.decryptUserId(encryptedHex);
        
        Integer originalUserId;
        try {
            originalUserId = Integer.parseInt(originalUserIdString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("復号されたIDが不正です: " + originalUserIdString);
        }

        //カードID検証
        String cardId = recordDTO.getCardId();
        CardsEntity card = cardsRepository.findByCardId(cardId)
                .orElseThrow(() -> new IllegalArgumentException("未登録のカードです: " + cardId));

        if (!Boolean.TRUE.equals(card.getIsActive())) {
            throw new IllegalStateException("無効化されたカードです。");
        }
        
        if (!card.getUserId().equals(originalUserId)) {
            throw new IllegalStateException("カード所有者とデータが不一致です(偽造の疑い)。");
        }

        //時刻の変換
        LocalDateTime scanTime;
        try {
            if (recordDTO.getReadTime() != null) {
                scanTime = LocalDateTime.parse(recordDTO.getReadTime());
            } else {
                scanTime = LocalDateTime.now();
            }
        } catch (Exception e) {
            scanTime = LocalDateTime.now();
        }

        //検証成功！データをDTOに詰めて返す
        return new VerifiedRecordDto(
                originalUserId,
                cardId,
                scanTime,
                recordDTO.getReaderId()
        );
    }
}