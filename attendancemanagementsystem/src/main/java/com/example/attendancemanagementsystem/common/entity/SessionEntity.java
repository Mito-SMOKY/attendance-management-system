package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "classsession")
public class SessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SessionID")
    private Integer sessionId;

    @Column(name = "TimeTableID")
    private Integer timeTableId;

    @Column(name = "ActualClassroomID")
    private Integer actualClassroomId;

    @Column(name = "TargetDepartmentID")
    private Integer targetDepartmentId;

    @Column(name = "TargetGrade")
    private Integer targetGrade;

    @Column(name = "SessionDate")
    private LocalDate sessionDate;

    @Column(name = "StartTime")
    private LocalDateTime startTime;

    @Column(name = "EndTime")
    private LocalDateTime endTime;

    @Column(name = "SessionStatus", columnDefinition = "TINYINT")
    private Integer sessionStatus;

    @Column(name = "Note")
    private String note;

    // --- Getters and Setters ---

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getTimeTableId() {
        return timeTableId;
    }

    public void setTimeTableId(Integer timeTableId) {
        this.timeTableId = timeTableId;
    }

    public Integer getActualClassroomId() {
        return actualClassroomId;
    }

    public void setActualClassroomId(Integer actualClassroomId) {
        this.actualClassroomId = actualClassroomId;
    }

    public Integer getTargetDepartmentId() {
        return targetDepartmentId;
    }

    public void setTargetDepartmentId(Integer targetDepartmentId) {
        this.targetDepartmentId = targetDepartmentId;
    }

    public Integer getTargetGrade() {
        return targetGrade;
    }

    public void setTargetGrade(Integer targetGrade) {
        this.targetGrade = targetGrade;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(Integer sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}