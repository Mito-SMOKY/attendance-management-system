package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @ManyToOne
    @JoinColumn(name = "UserID")
    private StudentEntity student;

    @ManyToOne
    @JoinColumn(name = "DepartmentID")
    private DepartmentEntity department;

    @Column(name = "AcademicYear")
    private Integer academicYear;

    @Column(name = "Grade")
    private Integer grade;

    @Column(name = "IsActive")
    private Boolean isActive;


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

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean isActive) {
        this.isActive = isActive;
    }
}