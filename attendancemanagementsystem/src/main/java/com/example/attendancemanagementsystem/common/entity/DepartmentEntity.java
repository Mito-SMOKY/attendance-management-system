package com.example.attendancemanagementsystem.common.entity;

import java.util.List; // List をインポート

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "department")
public class DepartmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DepartmentID")
    private Integer departmentId;

    @Column(name = "MajorID")
    private Integer majorId; // ※本来は MajorEntity への @ManyToOne

    @Column(name = "Class")
    private String className; // "Class" はJavaの予約語なので "className" に変更

    // --- 関連定義 ---

    // Department(1) 対 Enrollments(多)
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EnrollmentsEntity> enrollments;

    // Department(1) 対 Timetable(多)
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TimetableEntity> timetables;

    // --- コンストラクタ ---
    public DepartmentEntity() {
    }

    // --- ゲッター・セッター ---
    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getMajorId() {
        return majorId;
    }

    public void setMajorId(Integer majorId) {
        this.majorId = majorId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    // 関連のゲッター・セッター
    public List<EnrollmentsEntity> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<EnrollmentsEntity> enrollments) {
        this.enrollments = enrollments;
    }

    public List<TimetableEntity> getTimetables() {
        return timetables;
    }

    public void setTimetables(List<TimetableEntity> timetables) {
        this.timetables = timetables;
    }
}