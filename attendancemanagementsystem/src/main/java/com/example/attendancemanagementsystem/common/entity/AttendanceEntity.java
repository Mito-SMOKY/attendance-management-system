package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDateTime; // LocalDateTime をインポート

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance")
public class AttendanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AttendanceID")
    private Integer attendanceId;

    @Column(name = "StatusID")
    private Integer statusId; // ※本来は AttendanceStatusEntity への @ManyToOne

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    // --- 関連定義 ---

    // Attendance(多) 対 Timetable(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TimeTableID") // DBのFKカラム名
    private TimetableEntity timetable;

    // Attendance(多) 対 Student(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID") // DBのFKカラム名
    private StudentEntity student;

    // --- コンストラクタ ---
    public AttendanceEntity() {
    }

    // --- ゲッター・セッター ---
    public Integer getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Integer attendanceId) {
        this.attendanceId = attendanceId;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // 関連のゲッター・セッター
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