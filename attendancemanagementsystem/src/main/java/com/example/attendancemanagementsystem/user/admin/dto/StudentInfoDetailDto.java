package com.example.attendancemanagementsystem.user.admin.dto;

import java.util.List;

// 学生の詳細情報表示用DTO
public class StudentInfoDetailDto {
    private Integer studentId; 
    private String name;
    private String currentMonth;
    private List<String> periodHeaders;
    private List<SubjectSimpleDto> subjectList;
    private AttendanceSummaryDto summary;
    private List<DailyScheduleDto> scheduleList;
    private List<String> selectableMonths;

    // --- Getters & Setters ---
    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCurrentMonth() { return currentMonth; }
    public void setCurrentMonth(String currentMonth) { this.currentMonth = currentMonth; }

    public List<String> getPeriodHeaders() { return periodHeaders; }
    public void setPeriodHeaders(List<String> periodHeaders) { this.periodHeaders = periodHeaders; }

    public List<SubjectSimpleDto> getSubjectList() { return subjectList; }
    public void setSubjectList(List<SubjectSimpleDto> subjectList) { this.subjectList = subjectList; }

    public AttendanceSummaryDto getSummary() { return summary; }
    public void setSummary(AttendanceSummaryDto summary) { this.summary = summary; }

    public List<DailyScheduleDto> getScheduleList() { return scheduleList; }
    public void setScheduleList(List<DailyScheduleDto> scheduleList) { this.scheduleList = scheduleList; }

    public List<String> getSelectableMonths() { return selectableMonths; }
    public void setSelectableMonths(List<String> selectableMonths) { this.selectableMonths = selectableMonths; }


    // 科目リスト用のシンプルDTO
    public static class SubjectSimpleDto {
        private Integer subjectId; 
        private String subjectName;

        public Integer getSubjectId() { return subjectId; }
        public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    }

    // 出席状況の集計用DTO
    public static class AttendanceSummaryDto {
        private int attendanceCount = 0;
        private int absenceCount = 0;
        private int lateCount = 0;
        private int earlyLeaveCount = 0;
        private int suspensionCount = 0;
        private int publicAbsenceCount = 0;

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
    }

    // 日別スケジュール用DTO
    public static class DailyScheduleDto {
        private String dateStr;
        private boolean isAbsentDay;
        private List<PeriodDetailDto> periods;

        public String getDateStr() { return dateStr; }
        public void setDateStr(String dateStr) { this.dateStr = dateStr; }
        public boolean isAbsentDay() { return isAbsentDay; }
        public void setAbsentDay(boolean absentDay) { isAbsentDay = absentDay; }
        public List<PeriodDetailDto> getPeriods() { return periods; }
        public void setPeriods(List<PeriodDetailDto> periods) { this.periods = periods; }
    }

    // 各時間帯の詳細DTO
    public static class PeriodDetailDto {
        private int period;
        private String subjectName;
        private String statusIcon;
        private String statusClass;
        private boolean hasClass;

        public int getPeriod() { return period; }
        public void setPeriod(int period) { this.period = period; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        public String getStatusIcon() { return statusIcon; }
        public void setStatusIcon(String statusIcon) { this.statusIcon = statusIcon; }
        public String getStatusClass() { return statusClass; }
        public void setStatusClass(String statusClass) { this.statusClass = statusClass; }
        public boolean isHasClass() { return hasClass; }
        public void setHasClass(boolean hasClass) { this.hasClass = hasClass; }
    }
}