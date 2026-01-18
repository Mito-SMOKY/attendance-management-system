package com.example.attendancemanagementsystem.user.admin.dto;

import java.util.List;

public class AdminSubjectInfoDto {

    private Integer departmentId;
    private Integer subjectId;
    private Integer grade;

    private String subjectName;
    private String fiscalYear;
    private String courseAndGrade;
    private String displaySubjectId;

    private String teacherName;
    private String totalClasses;
    private String schedule;
    private String classroom;

    private List<StudentSimpleInfo> students;
    private List<SubjectOption> subjectList;

    public AdminSubjectInfoDto() {}

    // ★修正: 生徒ID (id) を追加
    public static class StudentSimpleInfo {
        private Integer id;   // ★追加
        private String name;
        
        public StudentSimpleInfo(Integer id, String name) { 
            this.id = id;
            this.name = name; 
        }
        
        // ★追加: getterが必須です
        public Integer getId() { return id; }
        public String getName() { return name; }
    }

    // 検索用オプション
    public static class SubjectOption {
        private Integer id;
        private String name;
        private Integer departmentId;
        private Integer grade;

        public SubjectOption(Integer id, String name, Integer departmentId, Integer grade) {
            this.id = id;
            this.name = name;
            this.departmentId = departmentId;
            this.grade = grade;
        }
        
        public Integer getId() { return id; }
        public String getName() { return name; }
        public Integer getDepartmentId() { return departmentId; }
        public Integer getGrade() { return grade; }
    }

    // --- Getters / Setters ---
    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(String fiscalYear) { this.fiscalYear = fiscalYear; }
    public String getCourseAndGrade() { return courseAndGrade; }
    public void setCourseAndGrade(String courseAndGrade) { this.courseAndGrade = courseAndGrade; }
    public String getDisplaySubjectId() { return displaySubjectId; }
    public void setDisplaySubjectId(String displaySubjectId) { this.displaySubjectId = displaySubjectId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getTotalClasses() { return totalClasses; }
    public void setTotalClasses(String totalClasses) { this.totalClasses = totalClasses; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public String getClassroom() { return classroom; }
    public void setClassroom(String classroom) { this.classroom = classroom; }
    public List<StudentSimpleInfo> getStudents() { return students; }
    public void setStudents(List<StudentSimpleInfo> students) { this.students = students; }
    public List<SubjectOption> getSubjectList() { return subjectList; }
    public void setSubjectList(List<SubjectOption> subjectList) { this.subjectList = subjectList; }
}