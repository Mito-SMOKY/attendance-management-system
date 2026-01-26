package com.example.attendancemanagementsystem.user.admin.dto;

import java.time.LocalDate;
import java.util.List;

public class StudentAttendanceUpdateDto {

    private Integer studentId;
    private List<DailyUpdateDto> updates;

    // --- Getters & Setters ---
    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public List<DailyUpdateDto> getUpdates() {
        return updates;
    }

    public void setUpdates(List<DailyUpdateDto> updates) {
        this.updates = updates;
    }

    // --- Inner Class ---
    public static class DailyUpdateDto {
        private LocalDate date;
        private Integer period; // 1限, 2限...
        private String status;  // "出席", "欠席" 等

        // --- Getters & Setters ---
        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public Integer getPeriod() {
            return period;
        }

        public void setPeriod(Integer period) {
            this.period = period;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}