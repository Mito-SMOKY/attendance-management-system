package com.example.attendancemanagementsystem.attendance.writer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WriterDto {

    @JsonProperty("login_id")
    private String loginId;

    @JsonProperty("card_id")
    private String cardId;

    // --- Getter / Setter ---
    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }
}