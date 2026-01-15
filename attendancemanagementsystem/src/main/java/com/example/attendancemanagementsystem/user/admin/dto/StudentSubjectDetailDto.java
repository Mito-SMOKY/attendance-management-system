package com.example.attendancemanagementsystem.user.admin.dto;

import java.util.List;

import lombok.Data;

@Data
public class StudentSubjectDetailDto {

    //生徒情報 
    private Integer studentId;
    private String studentName;

    // 科目情報
    private Integer subjectId;
    private String subjectName;
    private String classroomName;
    private String teacherName;
    private int totalClasses;      
    private String completedCount; 
    private String attendanceRate; 

    //出欠集計
    private int attendanceCount = 0;    
    private int absenceCount = 0;       
    private int lateCount = 0;          
    private int earlyLeaveCount = 0;    
    private int suspensionCount = 0;    
    private int publicAbsenceCount = 0; 
    private List<SubjectDailyRecord> dailyRecords;

    @Data
    public static class SubjectDailyRecord {

        // 日付
        private String dateStr;
        private String period1 = "-"; 
        private String period2 = "-";
        private String period3 = "-";
        private String period4 = "-";
    }
}