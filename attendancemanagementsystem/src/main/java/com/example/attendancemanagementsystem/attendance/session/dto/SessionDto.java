package com.example.attendancemanagementsystem.attendance.session.dto;

import lombok.Data;

// セッション情報のDTO
@Data
public class SessionDto {
    private Integer sessionId;
    private Integer userId;     
    private String studentName; 
    private String entryTime;   
    private String className;   
    private String classroomName;
    private String startTime;
    private String gradeClass;
    private Integer statusId;
    private Integer actualClassroomId;
    private Integer sessionStatus;
}
