package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    // ★修正: 重複エラー回避のため、こちらは読み取り専用にする
    @Column(name = "DataListID", insertable = false, updatable = false)
    private Integer dataListId;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- 関連定義 ---

    // Student(1) 対 Users(1)
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "UserID")
    private UsersEntity user;

    // Student(1) 対 Enrollments(多)
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EnrollmentsEntity> enrollments;

    // Student(1) 対 Attendance(多)
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AttendanceEntity> attendances;

    // ★こちらが DataListID の管理（保存・更新）を担当する
    @ManyToOne
    @JoinColumn(name = "DataListID", nullable = false)
    @JsonIgnore
    private Datalist datalist;

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

    public Datalist getDatalist() {
        return datalist;
    }

    public void setDatalist(Datalist datalist) {
        this.datalist = datalist;
    }
}