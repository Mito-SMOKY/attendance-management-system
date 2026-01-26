package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType; // 追加
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/**
 * 申請情報エンティティ
 * 休暇申請、削除申請など、全ての申請の親となるテーブル
 */
@Entity
@Table(name = "request")
public class RequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequestID")
    private Integer requestId;

    @Column(name = "RequestTypeID")
    private Integer requestTypeId;

    @Column(name = "RequesterUserID")
    private Integer requesterUserId;

    @Column(name = "RequestMessage")
    private String requestMessage;

    @Column(name = "Status")
    private Integer status;

    // 承認者（上位管理者）のID
    @Column(name = "ApproverUserID")
    private Integer approverId;

    @Column(name = "StartDate")
    private LocalDate startDate;

    @Column(name = "EndDate")
    private LocalDate endDate;

    @Column(name = "Periods")
    private String periods;

    @Column(name = "TargetStatusID")
    private Integer targetStatusId;

    @Column(name = "TargetDepartmentID")
    private Integer targetDepartmentId;

    @Column(name = "CreatedAt", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // 対象ユーザー（削除申請などで使用）
    // FetchType.EAGER に設定し、申請データを取得した際に対象者リストも確実に取得できるようにする
    // これにより LazyInitializationException を防ぐ
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "requesttargetuser", // 中間テーブル名
        joinColumns = @JoinColumn(name = "RequestID"), // こちら側の外部キー
        inverseJoinColumns = @JoinColumn(name = "UserID") // あちら側(Users)の外部キー
    )
    private List<UsersEntity> targetUsers = new ArrayList<>();

    // --- Getter / Setter ---
    
    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }

    public Integer getRequestTypeId() { return requestTypeId; }
    public void setRequestTypeId(Integer requestTypeId) { this.requestTypeId = requestTypeId; }

    public Integer getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(Integer requesterUserId) { this.requesterUserId = requesterUserId; }

    public String getRequestMessage() { return requestMessage; }
    public void setRequestMessage(String requestMessage) { this.requestMessage = requestMessage; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getApproverId() { return approverId; }
    public void setApproverId(Integer approverId) { this.approverId = approverId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getPeriods() { return periods; }
    public void setPeriods(String periods) { this.periods = periods; }

    public Integer getTargetStatusId() { return targetStatusId; }
    public void setTargetStatusId(Integer targetStatusId) { this.targetStatusId = targetStatusId; }

    public Integer getTargetDepartmentId() { return targetDepartmentId; }
    public void setTargetDepartmentId(Integer targetDepartmentId) { this.targetDepartmentId = targetDepartmentId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<UsersEntity> getTargetUsers() { return targetUsers; }
    public void setTargetUsers(List<UsersEntity> targetUsers) { this.targetUsers = targetUsers; }
    
    // 便利な追加メソッド: ユーザーをリストに追加する
    public void addTargetUser(UsersEntity user) {
        if (this.targetUsers == null) {
            this.targetUsers = new ArrayList<>();
        }
        this.targetUsers.add(user);
    }
}