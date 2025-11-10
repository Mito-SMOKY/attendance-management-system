package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users") // データベースのテーブル名を指定
public class UsersEntity {

    @Id // これはプライマリキーです
    @Column(name = "user_id") // DBのカラム名を指定
    private String userId;

    @Column(name = "user_type")
    private String userType;

    private String name;

    @Column(unique = true) // 重複を許可しない
    private String email;

    private String password;

    // --- ここから下はゲッターとセッター ---
    // (IDEの機能で自動生成できます)

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}