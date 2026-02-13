package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "updatelog")
public class UpdateLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    @Column(name = "target_user_id", nullable = false)
    private Integer targetUserId;

    @Column(name = "operator_id", nullable = false)
    private Integer operatorId;

    @Column(name = "change_content", columnDefinition = "TEXT")
    private String changeContent;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- コンストラクタ ---
    public UpdateLogEntity() {
    }

    public UpdateLogEntity(Integer targetUserId, Integer operatorId, String changeContent) {
        this.targetUserId = targetUserId;
        this.operatorId = operatorId;
        this.changeContent = changeContent;
    }

    // 保存前に自動で現在日時を入れる
    @PrePersist
    public void onPrePersist() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getter / Setter ---
    public Integer getLogId() {
        return logId;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
    }

    public Integer getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Integer targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Integer getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Integer operatorId) {
        this.operatorId = operatorId;
    }

    public String getChangeContent() {
        return changeContent;
    }

    public void setChangeContent(String changeContent) {
        this.changeContent = changeContent;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}