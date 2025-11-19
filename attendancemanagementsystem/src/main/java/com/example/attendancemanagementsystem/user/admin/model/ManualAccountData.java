package com.example.attendancemanagementsystem.user.admin.model;



public class ManualAccountData {
    private String studentNumber; // ログインID
    private String name;          // 氏名
    private String password;      // ★変更箇所: Email -> Password

    // --- Getter / Setter ---
    
    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
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