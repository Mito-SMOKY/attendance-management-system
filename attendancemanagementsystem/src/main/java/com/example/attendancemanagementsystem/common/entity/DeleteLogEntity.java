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
@Table(name = "deletelog")
public class DeleteLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    @Column(name = "target_user_id")
    private Integer targetUserId;

    @Column(name = "deleted_by")
    private Integer deletedBy;

    @Column(name = "deleted_at", updatable = false)
    private LocalDateTime deletedAt;

    // 保存前に現在時刻を自動セット
    @PrePersist
    public void onPrePersist() {
        this.deletedAt = LocalDateTime.now();
    }

    // --- コンストラクタ ---
    public DeleteLogEntity() {}

    public DeleteLogEntity(Integer targetUserId, Integer deletedBy) {
        this.targetUserId = targetUserId;
        this.deletedBy = deletedBy;
    }

    // --- Getter / Setter ---
    public Integer getLogId() { return logId; }
    public void setLogId(Integer logId) { this.logId = logId; }
    
    public Integer getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Integer targetUserId) { this.targetUserId = targetUserId; }
    
    public Integer getDeletedBy() { return deletedBy; }
    public void setDeletedBy(Integer deletedBy) { this.deletedBy = deletedBy; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}