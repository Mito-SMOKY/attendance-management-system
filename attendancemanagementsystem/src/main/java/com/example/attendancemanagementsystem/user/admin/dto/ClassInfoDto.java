package com.example.attendancemanagementsystem.user.admin.dto;

import java.util.List;
import com.example.attendancemanagementsystem.common.entity.*;

public class ClassInfoDto {

    // --- 表示用ヘッダー情報 ---
    private Integer sessionId;
    private String subjectName;
    private String teacherLoginId;
    private String teacherName;
    private String courseName;
    private String classroomName;
    private String timeSlotText;
    private String dateText;
    
    // --- 編集用: 現在の設定値 (ID) ---
    private Integer currentSubjectId;
    private Integer currentTeacherId;
    private Integer currentClassroomId;
    private Integer currentTimeSlotId;
    private String currentDateValue; 

    // --- 編集用: プルダウン表示用マスタデータ ---
    private List<SubjectEntity> allSubjects;
    private List<UsersEntity> allTeachers;
    private List<ClassroomEntity> allClassrooms;
    private List<TimeSlotEntity> allTimeSlots;
    private List<AttendanceStatusEntity> allStatuses;

    // --- 生徒リスト ---
    private List<StudentDetail> students;

    // --- Getter / Setter ---
    public Integer getSessionId() { return sessionId; }
    public void setSessionId(Integer sessionId) { this.sessionId = sessionId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getTeacherLoginId() { return teacherLoginId; }
    public void setTeacherLoginId(String teacherLoginId) { this.teacherLoginId = teacherLoginId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }
    public String getTimeSlotText() { return timeSlotText; }
    public void setTimeSlotText(String timeSlotText) { this.timeSlotText = timeSlotText; }
    public String getDateText() { return dateText; }
    public void setDateText(String dateText) { this.dateText = dateText; }

    public Integer getCurrentSubjectId() { return currentSubjectId; }
    public void setCurrentSubjectId(Integer currentSubjectId) { this.currentSubjectId = currentSubjectId; }
    public Integer getCurrentTeacherId() { return currentTeacherId; }
    public void setCurrentTeacherId(Integer currentTeacherId) { this.currentTeacherId = currentTeacherId; }
    public Integer getCurrentClassroomId() { return currentClassroomId; }
    public void setCurrentClassroomId(Integer currentClassroomId) { this.currentClassroomId = currentClassroomId; }
    public Integer getCurrentTimeSlotId() { return currentTimeSlotId; }
    public void setCurrentTimeSlotId(Integer currentTimeSlotId) { this.currentTimeSlotId = currentTimeSlotId; }
    public String getCurrentDateValue() { return currentDateValue; }
    public void setCurrentDateValue(String currentDateValue) { this.currentDateValue = currentDateValue; }

    public List<SubjectEntity> getAllSubjects() { return allSubjects; }
    public void setAllSubjects(List<SubjectEntity> allSubjects) { this.allSubjects = allSubjects; }
    public List<UsersEntity> getAllTeachers() { return allTeachers; }
    public void setAllTeachers(List<UsersEntity> allTeachers) { this.allTeachers = allTeachers; }
    public List<ClassroomEntity> getAllClassrooms() { return allClassrooms; }
    public void setAllClassrooms(List<ClassroomEntity> allClassrooms) { this.allClassrooms = allClassrooms; }
    public List<TimeSlotEntity> getAllTimeSlots() { return allTimeSlots; }
    public void setAllTimeSlots(List<TimeSlotEntity> allTimeSlots) { this.allTimeSlots = allTimeSlots; }
    public List<AttendanceStatusEntity> getAllStatuses() { return allStatuses; }
    public void setAllStatuses(List<AttendanceStatusEntity> allStatuses) { this.allStatuses = allStatuses; }

    public List<StudentDetail> getStudents() { return students; }
    public void setStudents(List<StudentDetail> students) { this.students = students; }

    // --- 内部クラス ---
    public static class StudentDetail {
        private Integer userId;
        private String studentNumber;
        private String grade;
        private String className;
        private String name;
        private String statusLabel;   
        private String statusStyle; 
        private Integer currentStatusId;

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
        public String getStudentNumber() { return studentNumber; }
        public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
        public String getStatusStyle() { return statusStyle; }
        public void setStatusStyle(String statusStyle) { this.statusStyle = statusStyle; }
        public Integer getCurrentStatusId() { return currentStatusId; }
        public void setCurrentStatusId(Integer currentStatusId) { this.currentStatusId = currentStatusId; }
    }
}