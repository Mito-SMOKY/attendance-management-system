package com.example.attendancemanagementsystem.user.superadmin.dto;

public class SuperAdminDetailDto {
    private Integer userId;
    private String name;
    private String email;
    private Integer adminLevelID; // 画面では 1(上位) or 0(一般) として扱います

    // --- Getter / Setter (Lombokがない場合用) ---
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAdminLevelID() { return adminLevelID; }
    public void setAdminLevelID(Integer adminLevelID) { this.adminLevelID = adminLevelID; }
}