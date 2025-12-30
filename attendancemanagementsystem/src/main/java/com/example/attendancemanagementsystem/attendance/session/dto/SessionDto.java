package com.example.attendancemanagementsystem.attendance.session.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SessionDto {
    private Integer sessionId;
    private Integer userId;     
    private String studentName; 
    private String entryTime;   
    private String className;   
    private String classroomName;
    private LocalDateTime startTime;
    private String gradeClass;
    private Integer statusId;
}