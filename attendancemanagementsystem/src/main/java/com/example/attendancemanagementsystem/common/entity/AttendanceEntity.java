package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDateTime;

// import javax.swing.plaf.TreeUI;

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

    // AttendanceStatus との紐づけ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StatusID", nullable = false)
    private AttendanceStatusEntity status;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 変数名を 'timeTable' にすることで getTimeTable() が生成されます
    @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "TimeTableID", nullable = false) 
    @JoinColumn(name = "TimeTableID", nullable = true) 
    private TimetableEntity timeTable;

    // UserID (Student) との紐づけ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false) 
    private StudentEntity student;

    // --- コンストラクタ ---
    public AttendanceEntity() {
    }

    // --- 保存時に日時を自動セット ---
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- ゲッター・セッター ---
    public Integer getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Integer attendanceId) {
        this.attendanceId = attendanceId;
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

    public TimetableEntity getTimeTable() {
        return timeTable;
    }

    public void setTimeTable(TimetableEntity timeTable) {
        this.timeTable = timeTable;
    }

    public StudentEntity getStudent() {
        return student;
    }

    public void setStudent(StudentEntity student) {
        this.student = student;
    }
}