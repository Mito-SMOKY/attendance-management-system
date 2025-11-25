package com.example.attendancemanagementsystem.attendance.record.service;

import com.example.attendancemanagementsystem.attendance.record.dto.RecordDTO;
import com.example.attendancemanagementsystem.common.entity.CardsEntity;
import com.example.attendancemanagementsystem.common.repository.CardsRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RecordService {

    private final CardsRepository cardsRepository;
    private final DecryptionService decryptionService;

    public RecordService(
        CardsRepository cardsRepository,
        DecryptionService decryptionService
    ) {
        this.cardsRepository = cardsRepository;
        this.decryptionService = decryptionService;
    }

    public void processAttendanceScan(RecordDTO recordDTO) {
        
        // --- 1. 復号処理 ---
        String encryptedHex = recordDTO.getUserId(); 
        String originalUserIdString = decryptionService.decryptUserId(encryptedHex);
        
        Integer originalUserId;
        try {
            originalUserId = Integer.parseInt(originalUserIdString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("復号されたユーザーIDが不正な形式です: " + originalUserIdString);
        }

        // --- 2. カードIDの取得 (修正箇所) ---
        // ★ Integerへの変換を廃止し、そのまま文字列として使用
        String cardId = recordDTO.getCardId(); 

        if (cardId == null || cardId.isEmpty()) {
             throw new IllegalArgumentException("カードIDが空です。");
        }

        // --- 3. データベース検証 ---
        
        // ★ String型のまま検索
        Optional<CardsEntity> cardOptional = cardsRepository.findByCardId(cardId);
        
        if (!cardOptional.isPresent()) {
            throw new IllegalArgumentException("指定されたカードID(" + cardId + ")が見つかりません。");
        }
        
        CardsEntity card = cardOptional.get();

        if (!card.getIsActive()) {
            throw new IllegalStateException("カードID(" + cardId + ")は無効化されています。");
        }
        
        if (!card.getUserId().equals(originalUserId)) {
            throw new IllegalStateException("カードに紐づくユーザーIDと復号されたユーザーID(" + originalUserId + ")が一致しません。");
        }

        // --- 4. ログ出力 ---
        System.out.println("==================================");
        System.out.println("NFCデータ 検証完了");
        System.out.println("  カードID: " + cardId); 
        System.out.println("  ユーザID: " + originalUserId);
        System.out.println("  読取日時: " + recordDTO.getReadTime());
        System.out.println("==================================");
    }
}