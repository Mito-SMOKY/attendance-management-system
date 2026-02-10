package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Immutable // ビューなので更新不可にする
@Table(name = "view_admin_history")
public class AdminHistoryEntity {

    @Id
    @Column(name = "history_id")
    private String historyId;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "target_name")
    private String targetName;

    @Column(name = "detail")
    private String detail;

    // --- Getter (Setterは不要ですがJPAのためにあってもOK) ---
    public String getHistoryId() { return historyId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getActionType() { return actionType; }
    public String getOperatorName() { return operatorName; }
    public String getTargetName() { return targetName; }
    public String getDetail() { return detail; }
}