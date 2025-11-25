package com.example.attendancemanagementsystem.attendance.record.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecordDTO {
    
    @JsonProperty("card_id")
    private String cardId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("timestamp")
    private String readTime;

    @JsonProperty("reader_id")
    private String readerId;

    // カードID
    public String getCardId() {
        return cardId;
    }
    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    // ユーザID
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // 読み込み時刻
    public String getReadTime() {
        return readTime;
    }
    public void setReadTime(String readTime) {
        this.readTime = readTime;
    }

    // ラズパイの識別番号
    public String getReaderId() {
        return readerId;
    }
    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }
}