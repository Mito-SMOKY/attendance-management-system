package com.example.attendancemanagementsystem.attendance.display.dto; // パッケージは適宜調整してください

import java.time.LocalDate;

public class DailyAttendanceDto {
    // 1. フィールド（データを入れる箱）
    private String date;
    private String status;

    // 2. コンストラクタ（データをセットするためのメソッド）
    // Service側で new AttendanceDto(日付, "出席") と書けるように
    public DailyAttendanceDto(LocalDate date, String status) {
        this.date = date.toString(); // LocalDateを文字列("2025-11-21")に変換して保存
        this.status = status;
    }

    // 3. Getter メソッド（重要）
    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    // 4. Setter メソッド
    public void setDate(String date) {
        this.date = date;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}