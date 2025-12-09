package com.example.attendancemanagementsystem.user.calendar.dto;

import java.time.LocalDate;

public class AttendanceDto {

    private String date;
    private String status;

    // --- コンストラクタ ---
    // Serviceから LocalDate と String を受け取れるようにします
    public AttendanceDto(LocalDate date, String status) {
        this.date = date.toString(); // "2025-11-25" のような文字列に変換
        this.status = status;
    }

    // --- Getter / Setter ---
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}