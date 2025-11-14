package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "department")
public class DepartmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DepartmentID")
    private Integer departmentId;

    @Column(name = "MajorID", nullable = false)
    private Integer majorId;

    @Column(name = "Class", nullable = false, length = 10)
    private String className; // "Class"は予約語のためフィールド名を変更

    // --- Getter ---
    public Integer getDepartmentId() {
        return departmentId;
    }

    public Integer getMajorId() {
        return majorId;
    }

    public String getClassName() {
        return className;
    }

    // --- Setter ---
    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public void setMajorId(Integer majorId) {
        this.majorId = majorId;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}