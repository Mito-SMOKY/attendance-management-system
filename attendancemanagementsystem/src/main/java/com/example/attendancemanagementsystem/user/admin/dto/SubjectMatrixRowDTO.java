package com.example.attendancemanagementsystem.user.admin.dto;
import java.util.Map;

public class SubjectMatrixRowDTO {
    private Integer subjectId;
    private String subjectName;
    private Integer grade; 
    private Map<String, Boolean> statusMap;

    public SubjectMatrixRowDTO(Integer subjectId, String subjectName, Integer grade, Map<String, Boolean> statusMap) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.grade = grade;
        this.statusMap = statusMap;
    }
    // Getter/Setter
    public Integer getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public Integer getGrade() { return grade; }
    public Map<String, Boolean> getStatusMap() { return statusMap; }
}