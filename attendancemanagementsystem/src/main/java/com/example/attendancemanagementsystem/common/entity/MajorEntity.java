package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "major")
public class MajorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MajorID")
    private Integer majorId;

    @Column(name = "MajorName")
    private String majorName;

    @Column(name = "CourseID")
    private Integer courseId;

    // コンストラクタ
    public MajorEntity() {}

    // Getter / Setter
    public Integer getMajorId() { return majorId; }
    public void setMajorId(Integer majorId) { this.majorId = majorId; }

    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
}