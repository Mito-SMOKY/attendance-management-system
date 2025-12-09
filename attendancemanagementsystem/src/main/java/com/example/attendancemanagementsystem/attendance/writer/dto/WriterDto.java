package com.example.attendancemanagementsystem.attendance.writer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WriterDto {

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("card_id")
    private String cardId;

    // --- Getter / Setter ---
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }
}