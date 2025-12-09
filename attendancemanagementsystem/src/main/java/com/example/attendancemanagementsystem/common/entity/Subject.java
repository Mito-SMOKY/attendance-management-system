package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "subject")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SubjectID")
    private Integer subjectId;

    @Column(name = "SubjectName", nullable = false)
    private String subjectName;

    @Column(name = "TotalCredits", nullable = false)
    private Integer totalCredits;

    @Column(name = "RequiredCredits", nullable = false)
    private Integer requiredCredits;

    // リレーション
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubjectFaculty> faculties;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DepartmentSubject> departments;

    // Getter/Setter
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public Integer getTotalCredits() { return totalCredits; }
    public void setTotalCredits(Integer totalCredits) { this.totalCredits = totalCredits; }
    public Integer getRequiredCredits() { return requiredCredits; }
    public void setRequiredCredits(Integer requiredCredits) { this.requiredCredits = requiredCredits; }
    public List<SubjectFaculty> getFaculties() { return faculties; }
    public void setFaculties(List<SubjectFaculty> faculties) { this.faculties = faculties; }
    public List<DepartmentSubject> getDepartments() { return departments; }
    public void setDepartments(List<DepartmentSubject> departments) { this.departments = departments; }
}