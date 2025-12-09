package com.example.attendancemanagementsystem.attendance.display.dto;

import java.util.List;

/**
 * 教科別詳細画面用のデータクラス
 */
public class SubjectAttendanceDto {

    // --- ヘッダー・基本情報 ---
    private Integer subjectId;
    private String subjectName;
    private String teacherName;
    private String classroom;

    // --- 基準値・計算用 ---
    private int requiredClasses;          // 必要コマ数
    private int maxAbsenceClasses;        // 欠席可能数
    private double currentAttendanceRate; // 現在の出席率

    // --- 集計データ ---
    private int presentClasses;         // 出席
    private int absentClasses;          // 欠席
    private int lateClasses;            // 遅刻
    private int officialAbsentClasses;  // 公欠
    private int officialPendingClasses; // 公欠申請中

    // --- カレンダーデータ (日別リスト) ---
    private List<DailyDetail> dailyAttendanceList;

    // ========================================================================
    // 内部クラス: 日別の詳細行
    // ========================================================================
    public static class DailyDetail {
        private String dateDay;             // 日付
        private List<String> classStatuses; // ステータスリスト
        private String classroom;           // 教室

        // コンストラクタ
        public DailyDetail(String dateDay, List<String> classStatuses, String classroom) {
            this.dateDay = dateDay;
            this.classStatuses = classStatuses;
            this.classroom = classroom;
        }

        // Getter / Setter
        public String getDateDay() { return dateDay; }
        public void setDateDay(String dateDay) { this.dateDay = dateDay; }
        public List<String> getClassStatuses() { return classStatuses; }
        public void setClassStatuses(List<String> classStatuses) { this.classStatuses = classStatuses; }
        public String getClassroom() { return classroom; }
        public void setClassroom(String classroom) { this.classroom = classroom; }
    }

    // ========================================================================
    // コンストラクタ
    // ========================================================================
    public SubjectAttendanceDto() {
    }

    // ========================================================================
    // Getter / Setter
    // ========================================================================
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getClassroom() { return classroom; }
    public void setClassroom(String classroom) { this.classroom = classroom; }
    public int getRequiredClasses() { return requiredClasses; }
    public void setRequiredClasses(int requiredClasses) { this.requiredClasses = requiredClasses; }
    public int getMaxAbsenceClasses() { return maxAbsenceClasses; }
    public void setMaxAbsenceClasses(int maxAbsenceClasses) { this.maxAbsenceClasses = maxAbsenceClasses; }
    public double getCurrentAttendanceRate() { return currentAttendanceRate; }
    public void setCurrentAttendanceRate(double currentAttendanceRate) { this.currentAttendanceRate = currentAttendanceRate; }
    public int getPresentClasses() { return presentClasses; }
    public void setPresentClasses(int presentClasses) { this.presentClasses = presentClasses; }
    public int getAbsentClasses() { return absentClasses; }
    public void setAbsentClasses(int absentClasses) { this.absentClasses = absentClasses; }
    public int getLateClasses() { return lateClasses; }
    public void setLateClasses(int lateClasses) { this.lateClasses = lateClasses; }
    public int getOfficialAbsentClasses() { return officialAbsentClasses; }
    public void setOfficialAbsentClasses(int officialAbsentClasses) { this.officialAbsentClasses = officialAbsentClasses; }
    public int getOfficialPendingClasses() { return officialPendingClasses; }
    public void setOfficialPendingClasses(int officialPendingClasses) { this.officialPendingClasses = officialPendingClasses; }
    public List<DailyDetail> getDailyAttendanceList() { return dailyAttendanceList; }
    public void setDailyAttendanceList(List<DailyDetail> dailyAttendanceList) { this.dailyAttendanceList = dailyAttendanceList; }
}