package com.example.attendancemanagementsystem.user.admin.model;

import java.util.List;

public class ManualAccountForm {

    // HTMLの name="dataListName" を受け取る
    private String dataListName;

    private Integer departmentId; // 学科・クラスID
    private Integer grade;        // 学年
    private Integer academicYear; // 年度

    // HTMLの name="name[]" を受け取る
    private List<String> name;
    
    // HTMLの name="studentId[]" を受け取る
    private List<String> studentId;
    
    // HTMLの name="affiliationId[]" を受け取る
    private List<String> affiliationId;

    private List<ManualAccountData> accounts;

    // --- Getter / Setter ---

    // 以前のエラー「undefined method getDataListName」を消すためにこの名前が必要です
    public String getDataListName() {
        return dataListName;
    }

    public void setDataListName(String dataListName) {
        this.dataListName = dataListName;
    }

    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public List<String> getStudentId() {
        return studentId;
    }

    public void setStudentId(List<String> studentId) {
        this.studentId = studentId;
    }

    public List<String> getAffiliationId() {
        return affiliationId;
    }

    public void setAffiliationId(List<String> affiliationId) {
        this.affiliationId = affiliationId;
    }

    // public String getDataListName() {
    //     return dataListName;
    // }

    // public void setDataListName(String dataListName) {
    //     this.dataListName = dataListName;
    // }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public Integer getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(Integer academicYear) {
        this.academicYear = academicYear;
    }

    public List<ManualAccountData> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<ManualAccountData> accounts) {
        this.accounts = accounts;
    }
}