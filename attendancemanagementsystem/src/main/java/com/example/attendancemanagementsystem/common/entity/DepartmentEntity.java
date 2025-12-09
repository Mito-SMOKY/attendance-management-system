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
@Table(name = "department")
public class DepartmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DepartmentId")
    private Integer departmentId;

    @ManyToOne
    @JoinColumn(name = "MajorID")
    private MajorEntity major;

    @Column(name = "Class")
    private String className; // DBカラム名 "Class" に対応

    // --- constructor ---
    public DepartmentEntity() {
    }

    // --- Getter / Setter ---
    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public MajorEntity getMajor() {
        return major;
    }

    public void setMajor(MajorEntity major) {
        this.major = major;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}