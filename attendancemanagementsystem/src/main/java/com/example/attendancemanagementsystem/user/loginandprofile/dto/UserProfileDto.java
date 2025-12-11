package com.example.attendancemanagementsystem.user.loginandprofile.dto;

public class UserProfileDto {
    private String name;
    private String role;
    private String timezone;

    public UserProfileDto(String name, String role, String timezone) {
        this.name = name;
        this.role = role;
        this.timezone = timezone;
    }
    // --- Getter/Setter ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}