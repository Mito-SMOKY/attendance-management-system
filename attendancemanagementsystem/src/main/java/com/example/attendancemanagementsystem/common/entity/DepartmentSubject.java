package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "departmentsubject")
public class DepartmentSubject {
    @EmbeddedId
    private DepartmentSubjectKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    // @MapsId("departmentId")
    // @JoinColumn(name = "DepartmentID")
    @JoinColumn(name = "DepartmentID", insertable = false, updatable = false)
    private DepartmentEntity department; // 既存の DepartmentEntity を使用

    @ManyToOne(fetch = FetchType.LAZY)
    // @MapsId("subjectId")
    @JoinColumn(name = "SubjectID", insertable = false, updatable = false)
    private SubjectEntity    subject;

    @Column(name = "Grade")
    private Integer grade;

    public DepartmentSubject() {}
    // Getter/Setter...
    public DepartmentSubjectKey getId() { return id; }
    public void setId(DepartmentSubjectKey id) { this.id = id; }
    public DepartmentEntity getDepartment() { return department; }
    public void setDepartment(DepartmentEntity department) { this.department = department; }
    public SubjectEntity getSubject() { return subject; }
    public void setSubject(SubjectEntity subject) { this.subject = subject; }
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
}