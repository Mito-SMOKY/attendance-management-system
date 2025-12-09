package com.example.attendancemanagementsystem.user.calendar.dto;

import java.time.LocalDate;

public class CalendarDto {

    private Integer calendarId;
    private String title;
    private String date;

    // --- コンストラクタ ---
    // Service側からの new CalendarDto(ID, タイトル, 日付データ) に対応
    public CalendarDto(Integer calendarId, String title, LocalDate date) {
        this.calendarId = calendarId;
        this.title = title;
        // LocalDate を String ("2025-11-25") に変換してセット
        this.date = date.toString(); 
    }

    // --- Getter / Setter ---
    public Integer getCalendarId() {
        return calendarId;
    }
    public void setCalendarId(Integer calendarId) {
        this.calendarId = calendarId;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
}