package com.example.attendancemanagementsystem.attendance.record.dto;

import java.time.LocalDateTime;

public class VerifiedRecordDto {
// 復号された正当なユーザーID (数値)
    private final Integer userId;

// カードID
private final String cardId;

    // 正確な打刻日時 (日付型)
    private final LocalDateTime scanTime;

    // どの教室か特定するための端末ID
    private final String readerId;

// --- コンストラクタ ---
    public VerifiedRecordDto(Integer userId, String cardId,LocalDateTime scanTime, String readerId) {
        this.userId = userId;
        this.cardId = cardId;
        this.scanTime = scanTime;
        this.readerId = readerId;
    }

    // --- Getter ---
    
    public Integer getUserId() {
        return userId;
    }

    public String getCardId() {
        return cardId;
    }

    public LocalDateTime getScanTime() {
        return scanTime;
    }

    public String getReaderId() {
        return readerId;
    }
}