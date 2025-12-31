package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "entrylog")
public class EntryLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EntryLogID")
    private Integer entryLogId;

    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "ClassroomID")
    private Integer classroomId;

    @Column(name = "EntryTime")
    private LocalDateTime entryTime;

    @Column(name = "IsProcessed")
    private Integer isProcessed = 0; 

    @Column(name = "ProcessedAt")
    private LocalDateTime processedAt;

    public EntryLogEntity() {}

    // --- Getter / Setter ---

    public Integer getEntryLogId() {
        return entryLogId;
    }

    public void setEntryLogId(Integer entryLogId) {
        this.entryLogId = entryLogId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(Integer classroomId) {
        this.classroomId = classroomId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public Integer getIsProcessed() {
        return isProcessed;
    }

    public void setIsProcessed(Integer isProcessed) {
        this.isProcessed = isProcessed;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
