package com.example.attendancemanagementsystem.common.entity;

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
@Table(name = "enrollments")
public class EnrollmentsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EnrollmentsID")
    private Integer enrollmentsId;

    @Column(name = "AcademicYear")
    private Integer academicYear;

    @Column(name = "Grade")
    private Integer grade;

    @Column(name = "IsActive")
    private boolean isActive;

    // --- 関連定義 ---

    // Enrollments(多) 対 Student(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID") // DBのFKカラム名
    private StudentEntity student;

    // Enrollments(多) 対 Department(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DepartmentID") // DBのFKカラム名
    private DepartmentEntity department;

    // --- constructor ---
    public EnrollmentsEntity() {
    }

    // --- Getter/Setter ---
    public Integer getEnrollmentsId() {
        return enrollmentsId;
    }

    public void setEnrollmentsId(Integer enrollmentsId) {
        this.enrollmentsId = enrollmentsId;
    }

    public Integer getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(Integer academicYear) {
        this.academicYear = academicYear;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    // 関連のゲッター・セッター
    public StudentEntity getStudent() {
        return student;
    }

    public void setStudent(StudentEntity student) {
        this.student = student;
    }

    public DepartmentEntity getDepartment() {
        return department;
    }

    public void setDepartment(DepartmentEntity department) {
        this.department = department;
    }
}