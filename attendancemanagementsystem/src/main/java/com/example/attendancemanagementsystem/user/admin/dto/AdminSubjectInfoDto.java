package com.example.attendancemanagementsystem.user.admin.dto;

import java.util.List;

public class AdminSubjectInfoDto {

    private Integer departmentId;
    private Integer subjectId;
    private Integer grade;

    // ヘッダー・基本情報
    private String subjectName;       // 講座名
    private String fiscalYear;        // 年度
    private String courseAndGrade;    // コース・学年
    private String displaySubjectId;  // 教科ID

    // 詳細情報
    private String teacherName;       // 担当教員
    private String totalClasses;      // 授業数
    private String schedule;          // 曜日・コマ
    private String classroom;         // 教室

    // 学生リスト
    private List<StudentSimpleInfo> students;
    
    // ★追加: 検索機能などで使うリスト（今は使わないかもしれませんが念のため）
    private List<SubjectOption> subjectList;

    public AdminSubjectInfoDto() {}

    // 学生用インナークラス
    public static class StudentSimpleInfo {
        private String name;
        public StudentSimpleInfo(String name) { this.name = name; }
        public String getName() { return name; }
    }

    // ★★★ ここが抜けていました！追加してください ★★★
    // 検索結果（オートコンプリート）を返すためのクラス
    public static class SubjectOption {
        private Integer id;           // subjectId
        private String name;          // subjectName
        private Integer departmentId; // 遷移先の学科ID
        private Integer grade;        // 遷移先の学年

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
    // ★★★ ここまで ★★★

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
    
    // subjectListのGetter/Setterも追加
    public List<SubjectOption> getSubjectList() { return subjectList; }
    public void setSubjectList(List<SubjectOption> subjectList) { this.subjectList = subjectList; }
}