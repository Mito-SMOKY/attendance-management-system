package com.example.attendancemanagementsystem.classroom.subject.dto;
import java.util.List;
import java.util.Map;

public class SubjectMatrixRowDTO {
    private Integer subjectId;
    private String subjectName; 
    private Integer grade;      
    private Map<String, Boolean> statusMap;
    
    // 教師情報 (複数対応)
    private List<Integer> teacherIds;
    private List<String> teacherNames;

    // 入力フォーム・表示用 
    private Integer majorId;
    private String majorName;
    private Integer departmentId;
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

    public List<Integer> getTeacherIds() { return teacherIds; }
    public void setTeacherIds(List<Integer> teacherIds) { this.teacherIds = teacherIds; }

    public List<String> getTeacherNames() { return teacherNames; }
    public void setTeacherNames(List<String> teacherNames) { this.teacherNames = teacherNames; }

    public Integer getMajorId() { return majorId; }
    public void setMajorId(Integer majorId) { this.majorId = majorId; }

    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }

    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}