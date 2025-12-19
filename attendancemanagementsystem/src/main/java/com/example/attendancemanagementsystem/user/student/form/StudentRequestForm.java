package com.example.attendancemanagementsystem.user.student.form;

import java.time.LocalDate;
import java.util.List;

//生徒からの申請入力値を受け取るフォーム
public class StudentRequestForm {
    
    private Integer startYear;
    private Integer startMonth;
    private Integer startDay;
    
    private Integer endYear;
    private Integer endMonth;
    private Integer endDay;
    
    private List<Integer> periods;
    private String reason;

    // --- 日付取得メソッド ---
    public LocalDate getStartDate() {
        if (startYear == null || startMonth == null || startDay == null) return null;
        return LocalDate.of(startYear, startMonth, startDay);
    }

    public LocalDate getEndDate() {
        if (endYear == null || endMonth == null || endDay == null) return null;
        return LocalDate.of(endYear, endMonth, endDay);
    }

    // --- Getter / Setter ---
    public Integer getStartYear() { return startYear; }
    public void setStartYear(Integer startYear) { this.startYear = startYear; }

    public Integer getStartMonth() { return startMonth; }
    public void setStartMonth(Integer startMonth) { this.startMonth = startMonth; }

    public Integer getStartDay() { return startDay; }
    public void setStartDay(Integer startDay) { this.startDay = startDay; }

    public Integer getEndYear() { return endYear; }
    public void setEndYear(Integer endYear) { this.endYear = endYear; }

    public Integer getEndMonth() { return endMonth; }
    public void setEndMonth(Integer endMonth) { this.endMonth = endMonth; }

    public Integer getEndDay() { return endDay; }
    public void setEndDay(Integer endDay) { this.endDay = endDay; }

    public List<Integer> getPeriods() { return periods; }
    public void setPeriods(List<Integer> periods) { this.periods = periods; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}