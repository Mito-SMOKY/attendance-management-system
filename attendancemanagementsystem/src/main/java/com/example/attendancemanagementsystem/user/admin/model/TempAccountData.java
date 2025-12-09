package com.example.attendancemanagementsystem.user.admin.model;


public class TempAccountData {
    private String loginId;
    private String name;
    private String password; // ★追加

    // --- Getter / Setter ---
    
    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}