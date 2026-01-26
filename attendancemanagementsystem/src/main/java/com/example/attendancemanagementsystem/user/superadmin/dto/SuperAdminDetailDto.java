package com.example.attendancemanagementsystem.user.superadmin.dto;

public class SuperAdminDetailDto {
    private Integer userId;
    
    // ★追加: ログインID
    private String loginId;
    
    private String name;
    private String email;
    private Integer adminLevelID;
    private String currentAdminPassword;

    // --- Getter / Setter ---
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getAdminLevelID() { return adminLevelID; }
    public void setAdminLevelID(Integer adminLevelID) { this.adminLevelID = adminLevelID; }

    public String getCurrentAdminPassword() { return currentAdminPassword; }
    public void setCurrentAdminPassword(String currentAdminPassword) { this.currentAdminPassword = currentAdminPassword;}
}