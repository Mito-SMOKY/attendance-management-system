package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDateTime; // LocalDateTime をインポート
import java.util.List; // List をインポート

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "student")
public class StudentEntity {

    @Id
    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "StudentStatusID")
    private Integer studentStatusId;

    @Column(name = "DataListID")
    private Integer dataListId;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    // --- 関連定義 ---

    // Student(1) 対 Users(1)
    // @MapsId を使い、このエンティティのPK(userId)が、関連する"users"エンティティのPKからマッピングされることを示す
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "UserID") // DBのFKカラム名
    private UsersEntity user;

    // Student(1) 対 Enrollments(多)
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EnrollmentsEntity> enrollments;

    // Student(1) 対 Attendance(多)
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AttendanceEntity> attendances;

    // --- constructor ---
    public StudentEntity() {
    }

    // --- Getter/Setter ---
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getStudentStatusId() {
        return studentStatusId;
    }

    public void setStudentStatusId(Integer studentStatusId) {
        this.studentStatusId = studentStatusId;
    }

    public Integer getDataListId() {
        return dataListId;
    }

    public void setDataListId(Integer dataListId) {
        this.dataListId = dataListId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // 関連のゲッター・セッター
    public UsersEntity getUser() {
        return user;
    }

    public void setUser(UsersEntity user) {
        this.user = user;
    }

    public List<EnrollmentsEntity> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<EnrollmentsEntity> enrollments) {
        this.enrollments = enrollments;
    }

    public List<AttendanceEntity> getAttendances() {
        return attendances;
    }

    public void setAttendances(List<AttendanceEntity> attendances) {
        this.attendances = attendances;
    }
}