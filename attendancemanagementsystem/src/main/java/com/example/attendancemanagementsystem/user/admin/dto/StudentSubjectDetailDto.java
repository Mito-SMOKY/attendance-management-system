package com.example.attendancemanagementsystem.user.admin.dto;

import java.util.List;

public class StudentSubjectDetailDto {

    // 生徒情報 
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
    
    //教科詳細画面へのリンク用
    private Integer departmentId;
    private Integer grade;

    // 出欠集計
    private int attendanceCount = 0;    
    private int absenceCount = 0;       
    private int lateCount = 0;          
    private int earlyLeaveCount = 0;    
    private int suspensionCount = 0;    
    private int publicAbsenceCount = 0; 
    private List<SubjectDailyRecord> dailyRecords;

    // --- Getters & Setters ---

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public int getTotalClasses() { return totalClasses; }
    public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }

    public String getCompletedCount() { return completedCount; }
    public void setCompletedCount(String completedCount) { this.completedCount = completedCount; }

    public String getAttendanceRate() { return attendanceRate; }
    public void setAttendanceRate(String attendanceRate) { this.attendanceRate = attendanceRate; }

    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public int getAttendanceCount() { return attendanceCount; }
    public void setAttendanceCount(int attendanceCount) { this.attendanceCount = attendanceCount; }

    public int getAbsenceCount() { return absenceCount; }
    public void setAbsenceCount(int absenceCount) { this.absenceCount = absenceCount; }

    public int getLateCount() { return lateCount; }
    public void setLateCount(int lateCount) { this.lateCount = lateCount; }

    public int getEarlyLeaveCount() { return earlyLeaveCount; }
    public void setEarlyLeaveCount(int earlyLeaveCount) { this.earlyLeaveCount = earlyLeaveCount; }

    public int getSuspensionCount() { return suspensionCount; }
    public void setSuspensionCount(int suspensionCount) { this.suspensionCount = suspensionCount; }

    public int getPublicAbsenceCount() { return publicAbsenceCount; }
    public void setPublicAbsenceCount(int publicAbsenceCount) { this.publicAbsenceCount = publicAbsenceCount; }

    public List<SubjectDailyRecord> getDailyRecords() { return dailyRecords; }
    public void setDailyRecords(List<SubjectDailyRecord> dailyRecords) { this.dailyRecords = dailyRecords; }

    // インナークラス
    public static class SubjectDailyRecord {
        private String dateStr;
        private String period1 = "-"; 
        private String period2 = "-";
        private String period3 = "-";
        private String period4 = "-";

        public String getDateStr() { return dateStr; }
        public void setDateStr(String dateStr) { this.dateStr = dateStr; }
        public String getPeriod1() { return period1; }
        public void setPeriod1(String period1) { this.period1 = period1; }
        public String getPeriod2() { return period2; }
        public void setPeriod2(String period2) { this.period2 = period2; }
        public String getPeriod3() { return period3; }
        public void setPeriod3(String period3) { this.period3 = period3; }
        public String getPeriod4() { return period4; }
        public void setPeriod4(String period4) { this.period4 = period4; }
    }
}