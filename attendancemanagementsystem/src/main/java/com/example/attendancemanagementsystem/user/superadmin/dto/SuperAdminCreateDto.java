package com.example.attendancemanagementsystem.user.superadmin.dto;

public class SuperAdminCreateDto {
    private String loginId;
    private String name;
    // private String email; // 削除
    private String password;
    private Integer adminLevelID;

    // --- Getter / Setter ---
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    // EmailのGetter/Setter削除
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getAdminLevelID() { return adminLevelID; }
    public void setAdminLevelID(Integer adminLevelID) { this.adminLevelID = adminLevelID; }
}