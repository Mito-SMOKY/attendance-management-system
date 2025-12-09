package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
public class CardsEntity {

    @Id
    @Column(name = "CardID")
    private String cardId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "is_Active")
    private Boolean isActive;

    @Column(name = "issued_Date")
    private LocalDateTime issuedDate;

    // --- constructor ---
    public CardsEntity() {
    }

    public CardsEntity(String cardId, Integer userId, Boolean isActive, LocalDateTime issuedDate) {
        this.cardId = cardId;
        this.userId = userId;
        this.isActive = isActive;
        this.issuedDate = issuedDate;
    }
    
    // --- Getter/Setter ---
    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDateTime issuedDate) {
        this.issuedDate = issuedDate;
    }
}


