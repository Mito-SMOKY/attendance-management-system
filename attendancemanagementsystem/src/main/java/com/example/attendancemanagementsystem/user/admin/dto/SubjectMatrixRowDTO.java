package com.example.attendancemanagementsystem.user.admin.dto;
import java.util.Map;

public class SubjectMatrixRowDTO {
    private Integer subjectId;
    private String subjectName;
    private Integer grade; 
    private Map<String, Boolean> statusMap;
    
    // ★追加: 教科担当の教師情報
    private Integer teacherId;
    private String teacherName;

    private Integer courseCount;

    // 既存のコンストラクタ（マトリクス画面用として残すか、下のように修正）
    public SubjectMatrixRowDTO(Integer subjectId, String subjectName, Integer grade, Map<String, Boolean> statusMap) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.grade = grade;
        this.statusMap = statusMap;
    }
    
    // ★追加: 空のコンストラクタ（セッター注入用）
    public SubjectMatrixRowDTO() {}

    // Getter/Setter
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; } // Setter追加
    
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; } // Setter追加
    
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    
    public Map<String, Boolean> getStatusMap() { return statusMap; }
    public void setStatusMap(Map<String, Boolean> statusMap) { this.statusMap = statusMap; }

    // ★追加: 教師情報のGetter/Setter
    public Integer getTeacherId() { return teacherId; }
    public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public Integer getCourseCount() { return courseCount; }
    public void setCourseCount(Integer courseCount) { this.courseCount = courseCount; }
}
