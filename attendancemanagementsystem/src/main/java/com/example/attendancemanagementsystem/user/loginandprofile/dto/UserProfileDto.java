package com.example.attendancemanagementsystem.user.loginandprofile.dto;

public class UserProfileDto {
    private Integer userId;
    private String name;
    private String role;
    private String email;

    public UserProfileDto(Integer userId,String name, String role, String email) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.email = email;
    }

    // --- Getter/Setter ---
    public Integer getUserId() { 
        return userId; 
    }

    public void setUserId(Integer userId) { 
        this.userId = userId; 
    }

    public String getName() { 
        return name; 
    }

    public void setName(String name) { 
        this.name = name; 
    }

    public String getRole() { 
        return role; 
    }

    public void setRole(String role) { 
        this.role = role; 
    }

    public String getEmail() { 
        return email; 
    }

    public void setEmail(String email) { 
        this.email = email; 
    }
}