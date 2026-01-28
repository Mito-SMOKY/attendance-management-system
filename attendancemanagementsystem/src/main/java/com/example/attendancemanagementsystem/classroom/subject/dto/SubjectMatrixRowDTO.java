package com.example.attendancemanagementsystem.classroom.subject.dto;
import java.util.Map;

public class SubjectMatrixRowDTO {
    private Integer subjectId;
    private String subjectName; 
    private Integer grade;      
    private Map<String, Boolean> statusMap;
    
    // 教師情報
    private Integer teacherId;
    private String teacherName;
    private Integer courseCount;

    // 入力フォーム用 
    private Integer majorId;
    private Integer departmentId;
    private String rawSubjectName;

    // 情報マスタ一覧表示用フィールド
    private String majorName;
    private String className;

    public SubjectMatrixRowDTO() {}

    // --- Getter/Setter ---
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    
    public Map<String, Boolean> getStatusMap() { return statusMap; }
    public void setStatusMap(Map<String, Boolean> statusMap) { this.statusMap = statusMap; }

    public Integer getTeacherId() { return teacherId; }
    public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public Integer getCourseCount() { return courseCount; }
    public void setCourseCount(Integer courseCount) { this.courseCount = courseCount; }

    public Integer getMajorId() { return majorId; }
    public void setMajorId(Integer majorId) { this.majorId = majorId; }

    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public String getRawSubjectName() { return rawSubjectName; }
    public void setRawSubjectName(String rawSubjectName) { this.rawSubjectName = rawSubjectName; }

    // ★追加分のGetter/Setter
    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}