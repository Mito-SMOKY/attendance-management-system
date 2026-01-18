package com.example.attendancemanagementsystem.user.admin.dto;

public class AdminSubjectListDto {
    private Integer subjectId;
    private String subjectName;
    private String courseName;
    private Integer grade;
    private String className;

    // コンストラクタ
    public AdminSubjectListDto(Integer subjectId, String subjectName, String courseName, Integer grade, String className) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.courseName = courseName;
        this.grade = grade;
        this.className = className;
    }

    public AdminSubjectListDto() {}

    // --- Getter / Setter ---
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    // --- 画面表示用メソッド ---

    // 全体のフォーマット: {情報システム科 2年A組} Javaプログラミング
    public String getFormattedName() {
        return String.format("{%s %d年%s} %s", 
            courseName != null ? courseName : "",
            grade != null ? grade : 0,
            className != null ? className : "",
            subjectName
        );
    }

    // ★追加: クラス情報部分だけを返す: {情報システム科 2年A組}
    public String getClassInfoStr() {
        return String.format("{%s %d%s}", 
            courseName != null ? courseName : "",
            grade != null ? grade : 0,
            className != null ? className : ""
        );
    }
}