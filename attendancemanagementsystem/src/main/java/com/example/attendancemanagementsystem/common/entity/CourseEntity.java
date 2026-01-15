package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "course") // データベースのテーブル名
@Data
public class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CourseID") // データベースの列名
    private Integer courseId;

    @Column(name = "CourseName", nullable = false)
    private String courseName;

    // --- ここから追加・修正 ---

    // データベースに追加された MajorID 列に対応
    @Column(name = "MajorID")
    private Integer majorId;

    // 学科(Major)との多対一のリレーション
    // 「このコースは、ある1つの学科に属する」
    @ManyToOne
    @JoinColumn(name = "MajorID", insertable = false, updatable = false)
    private MajorEntity major;

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
}