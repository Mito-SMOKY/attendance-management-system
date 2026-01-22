package com.example.attendancemanagementsystem.user.admin.model;

import java.util.List;

public class DatalistForm {
    private String datalistName;
    private List<TempAccountData> tempAccounts;

    // ★以下を追加してください
    private Integer departmentId; // 学科ID
    private Integer grade;        // 学年
    private Integer academicYear; // 年度

    public String getDatalistName() { return datalistName; }
    public void setDatalistName(String datalistName) { this.datalistName = datalistName; }

    public List<TempAccountData> getTempAccounts() { return tempAccounts; }
    public void setTempAccounts(List<TempAccountData> tempAccounts) { this.tempAccounts = tempAccounts; }

    // ★追加分のGetter/Setter
    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public Integer getAcademicYear() { return academicYear; }
    public void setAcademicYear(Integer academicYear) { this.academicYear = academicYear; }
}