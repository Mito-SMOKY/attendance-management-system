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
@Table(name = "creationlog")
public class CreationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogID")
    private Integer logId;

    @Column(name = "OperatorID")
    private Integer operatorId;

    @Column(name = "TargetName")
    private String targetName;

    @Column(name = "TargetLoginId")
    private String targetLoginId;

    @Column(name = "Password")
    private String password;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onPrePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // --- コンストラクタ ---
    public CreationLogEntity() {}

    public CreationLogEntity(Integer operatorId, String targetName, String targetLoginId, String password) {
        this.operatorId = operatorId;
        this.targetName = targetName;
        this.targetLoginId = targetLoginId;
        this.password = password;
    }

    // --- Getter / Setter ---
    public Integer getLogId() { return logId; }
    public void setLogId(Integer logId) { this.logId = logId; }
    public Integer getOperatorId() { return operatorId; }
    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getTargetLoginId() { return targetLoginId; }
    public void setTargetLoginId(String targetLoginId) { this.targetLoginId = targetLoginId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}