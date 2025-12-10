package com.example.attendancemanagementsystem.user.loginandprofile.dto;

public class ProfileSubjectDto {
    private Integer subjectId;
    private String subjectName;

    public ProfileSubjectDto(Integer subjectId, String subjectName) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
    }

    // --- Getter/Setter ---
    public Integer getSubjectID() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
}