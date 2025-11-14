package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "enrollments")
public class EnrollmentsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EnrollmentsID")
    private Integer enrollmentsId;

    @Column(name = "UserID", nullable = false)
    private Integer userId;

    @Column(name = "DepartmentID", nullable = false)
    private Integer departmentId;

    @Column(name = "AcademicYear", nullable = false)
    private Integer academicYear;

    @Column(name = "Grade", nullable = false)
    private Integer grade;

    @Column(name = "IsActive", nullable = false)
    private boolean isActive;

   // --- Getter ---
    public Integer getEnrollmentsId() {
        return enrollmentsId;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public Integer getAcademicYear() {
        return academicYear;
    }

    public Integer getGrade() {
        return grade;
    }

    // boolean型のGetterは isXxx() が慣習
    public boolean isActive() {
        return isActive;
    }

    // --- Setter ---
    public void setEnrollmentsId(Integer enrollmentsId) {
        this.enrollmentsId = enrollmentsId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public void setAcademicYear(Integer academicYear) {
        this.academicYear = academicYear;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}