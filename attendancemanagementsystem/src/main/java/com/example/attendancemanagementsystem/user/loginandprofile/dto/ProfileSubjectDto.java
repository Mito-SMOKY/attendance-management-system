package com.example.attendancemanagementsystem.user.loginandprofile.dto;

public class ProfileSubjectDto {
    private Integer subjectId;
    private String subjectName;
    private Integer departmentId; 
    private Integer grade;        

    // 学生用コンストラクタ
    public ProfileSubjectDto(Integer subjectId, String subjectName) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
    }

    // 管理者用コンストラクタ
    public ProfileSubjectDto(Integer subjectId, String subjectName, Integer departmentId, Integer grade) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.departmentId = departmentId;
        this.grade = grade;
    }

    // --- Getter/Setter ---
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
}