package com.example.attendancemanagementsystem.Dummymodel;

import java.util.List;

/**
 * 特定教科の出席詳細画面のメインデータクラス
 */
public class AttendanceData {

    // --- 左側コンテナの情報 (Subject, Classroom, Userテーブルなどから取得) ---
    private final String subjectName;       // 教科名
    private final String classroom;         // 教室
    private final String teacherName;       // 担当教師
    private final int requiredClasses;      // 必要コマ数 (Subjectテーブルから)
    private final double currentAttendanceRate; // 現在の出席率 (Attendanceテーブルから算出)
    private final int maxAbsenceClasses;    // 欠席可能日数（コマ）

    // --- 右側サマリーコンテナの情報 (Attendanceテーブルから集計) ---
    private final int totalDays;            // 対象月の日数
    private final int presentClasses;       // 出席コマ数
    private final int absentClasses;        // 欠席コマ数
    private final int lateClasses;          // 遅刻コマ数
    private final int officialAbsentClasses; // 公欠コマ数
    private final int officialPendingClasses;// 公欠候補コマ数
    
    // --- カレンダー形式の出席詳細 (TimeTable, Attendanceテーブルから取得) ---
    private final List<DailyAttendance> dailyAttendanceList;

    // コンストラクタ
    public AttendanceData(String subjectName, String classroom, String teacherName, int requiredClasses, 
                          double currentAttendanceRate, int maxAbsenceClasses, int totalDays, 
                          int presentClasses, int absentClasses, int lateClasses, int officialAbsentClasses, 
                          int officialPendingClasses, List<DailyAttendance> dailyAttendanceList) {
        this.subjectName = subjectName;
        this.classroom = classroom;
        this.teacherName = teacherName;
        this.requiredClasses = requiredClasses;
        this.currentAttendanceRate = currentAttendanceRate;
        this.maxAbsenceClasses = maxAbsenceClasses;
        this.totalDays = totalDays;
        this.presentClasses = presentClasses;
        this.absentClasses = absentClasses;
        this.lateClasses = lateClasses;
        this.officialAbsentClasses = officialAbsentClasses;
        this.officialPendingClasses = officialPendingClasses;
        this.dailyAttendanceList = dailyAttendanceList;
    }

    // --- ゲッターメソッド（省略しない） ---
    public String getSubjectName() { return subjectName; }
    public String getClassroom() { return classroom; }
    public String getTeacherName() { return teacherName; }
    public int getRequiredClasses() { return requiredClasses; }
    public double getCurrentAttendanceRate() { return currentAttendanceRate; }
    public int getMaxAbsenceClasses() { return maxAbsenceClasses; }
    public int getTotalDays() { return totalDays; }
    public int getPresentClasses() { return presentClasses; }
    public int getAbsentClasses() { return absentClasses; }
    public int getLateClasses() { return lateClasses; }
    public int getOfficialAbsentClasses() { return officialAbsentClasses; }
    public int getOfficialPendingClasses() { return officialPendingClasses; }
    public List<DailyAttendance> getDailyAttendanceList() { return dailyAttendanceList; }
}