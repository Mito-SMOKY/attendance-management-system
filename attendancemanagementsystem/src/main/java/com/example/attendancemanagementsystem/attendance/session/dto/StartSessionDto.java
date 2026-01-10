package com.example.attendancemanagementsystem.attendance.session.dto;

import java.time.LocalDate;

public class StartSessionDto {
    private LocalDate date;
    private Integer slotId;
    private Integer classroomId;
    private Integer subjectId;
    private Integer departmentId; 
    private Integer targetGrade;

    // Getter/Setter 

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getSlotId() {
        return slotId;
    }

    public void setSlotId(Integer slotId) {
        this.slotId = slotId;
    }

    public Integer getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(Integer classroomId) {
        this.classroomId = classroomId;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getTargetGrade() {
        return targetGrade;
    }

    public void setTargetGrade(Integer targetGrade) {
        this.targetGrade = targetGrade;
    }
}