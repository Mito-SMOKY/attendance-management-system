package com.example.attendancemanagementsystem.Dummymodel;

import java.util.List;

/**
 * 1日分の出欠情報と教室情報を保持するクラス (TimeTable, Attendance, Classroomテーブルに対応)
 */
public class DailyAttendance {
    private final String dateDay;               // 日付 (例: "11/1(金)")
    private final List<String> classStatuses;   // 各時限の出欠ステータス (例: ["○", "○", "-", "○"])
    private final String classroom;             // 教室 (例: 231)

    // コンストラクタ
    public DailyAttendance(String dateDay, List<String> classStatuses, String classroom) {
        this.dateDay = dateDay;
        this.classStatuses = classStatuses;
        this.classroom = classroom;
    }

    // --- ゲッターメソッド（省略しない） ---
    public String getDateDay() { return dateDay; }
    public List<String> getClassStatuses() { return classStatuses; }
    public String getClassroom() { return classroom; }
}