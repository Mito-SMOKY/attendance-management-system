package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "datalist_detail")
public class DatalistDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DatalistDetailID")
    private Integer datalistDetailId;

    @Column(name = "DatalistID")
    private Integer datalistId;

    // ★変更: カラム名に合わせて LoginID に変更
    @Column(name = "LoginID")
    private String loginId;

    @Column(name = "Name")
    private String name;

    @Column(name = "AcademicYear")
    private Integer academicYear;

    @Column(name = "Grade")
    private Integer grade;

    @Column(name = "MajorName")
    private String majorName;

    @Column(name = "ClassName")
    private String className;

    // --- Getter / Setter ---
    public Integer getDatalistDetailId() { return datalistDetailId; }
    public void setDatalistDetailId(Integer datalistDetailId) { this.datalistDetailId = datalistDetailId; }

    public Integer getDatalistId() { return datalistId; }
    public void setDatalistId(Integer datalistId) { this.datalistId = datalistId; }

    // ★変更: Getter/Setter名も変更
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAcademicYear() { return academicYear; }
    public void setAcademicYear(Integer academicYear) { this.academicYear = academicYear; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}