package com.example.attendancemanagementsystem.common.dto;

public class AttendanceMetricsDto {
    private double attendanceRate; 
    private int remainingAbsenceDays; 

    public AttendanceMetricsDto(double attendanceRate, int remainingAbsenceDays) {
        this.attendanceRate = attendanceRate;
        this.remainingAbsenceDays = remainingAbsenceDays;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public int getAttendanceRatePercent() {
        return (int) (attendanceRate * 100);
    }

    public int getRemainingAbsenceDays() {
        return remainingAbsenceDays;
    }
}