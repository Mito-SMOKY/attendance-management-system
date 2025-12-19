package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "requesttargetuser",
        joinColumns = @JoinColumn(name = "RequestID"),
        inverseJoinColumns = @JoinColumn(name = "UserID")
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
    
    public void addTargetUser(UsersEntity user) {
        this.targetUsers.add(user);
    }
}