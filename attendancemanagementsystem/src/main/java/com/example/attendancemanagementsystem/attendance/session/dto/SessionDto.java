package com.example.attendancemanagementsystem.attendance.session.dto;


public class SessionDto {
    private Integer sessionId;
    private Integer userId;     
    private String studentName; 
    private String entryTime;   
    private String className;   
    private String classroomName;
    private String startTime;
    private String gradeClass;
    private String endTime;
    private Integer actualClassroomId;
    private Boolean sessionFlag;
    private String subjectName;
    private Integer statusId;

    // Getter/Sette

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(String entryTime) {
        this.entryTime = entryTime;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassroomName() {
        return classroomName;
    }

    public void setClassroomName(String classroomName) {
        this.classroomName = classroomName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getGradeClass() {
        return gradeClass;
    }

    public void setGradeClass(String gradeClass) {
        this.gradeClass = gradeClass;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getActualClassroomId() {
        return actualClassroomId;
    }

    public void setActualClassroomId(Integer actualClassroomId) {
        this.actualClassroomId = actualClassroomId;
    }

    public Boolean getSessionFlag() {
        return sessionFlag;
    }

    public void setSessionFlag(Boolean sessionFlag) {
        this.sessionFlag = sessionFlag;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }
}

