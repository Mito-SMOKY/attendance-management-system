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

    // cardUid
    public String getCardId() {
        return cardId;
    }
    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    // encryptedUserId
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // timestamp
    public String getReadTime() {
        return readTime;
    }
    public void setReadTime(String readTime) {
        this.readTime = readTime;
    }

    // readerId
    public String getReaderId() {
        return readerId;
    }
    public void setReaderId(String readerId) {
        this.readerId = readerId;
    }
}