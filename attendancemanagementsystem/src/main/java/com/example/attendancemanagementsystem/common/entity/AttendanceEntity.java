package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance")
public class AttendanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AttendanceID")
    private Integer attendanceId;

    @Column(name = "SessionID")
    private Integer sessionId;

    // AttendanceStatus との紐づけ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StatusID", nullable = false)
    private AttendanceStatusEntity status;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 変数名を 'timeTable' にすることで getTimeTable() が生成されます
    @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "TimeTableID", nullable = false) 
    @JoinColumn(name = "TimetableID", nullable = true) 
    private TimetableEntity timetable;

    // UserID (Student) との紐づけ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false) 
    private StudentEntity student;

    // --- constructor ---
    public AttendanceEntity() {
    }

    // --- 保存時に日時を自動セット ---
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getter/Setter ---
    public Integer getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Integer attendanceId) {
        this.attendanceId = attendanceId;
    }

    public Integer getSessionId() { 
        return sessionId; 
    }

    public void setSessionId(Integer sessionId) { 
        this.sessionId = sessionId; 
    }

    public AttendanceStatusEntity getStatus() {
        return status;
    }

    public void setStatusId(AttendanceStatusEntity status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public TimetableEntity getTimetable() {
        return timetable;
    }

    public void setTimetable(TimetableEntity timetable) {
        this.timetable = timetable;
    }

    public StudentEntity getStudent() {
        return student;
    }

    public void setStudent(StudentEntity student) {
        this.student = student;
    }
}