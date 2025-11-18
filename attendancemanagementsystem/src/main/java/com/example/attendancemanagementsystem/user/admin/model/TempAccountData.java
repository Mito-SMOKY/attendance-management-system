package com.example.attendancemanagementsystem.user.admin.model;


public class TempAccountData {
    private String loginId; // studentNumber から変更
    private String name;
    
    // Getter / Setter
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}