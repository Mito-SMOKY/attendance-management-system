package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "administrator")
public class AdministratorEntity {

    @Id
    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "AdminLevelID", nullable = false)
    private Integer adminLevelId;

    // ★追加: Usersテーブルと結合して名前を取得できるようにする設定
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID")
    @MapsId // PK(UserID)を共有する設定
    private UsersEntity user;

    // --- Getter / Setter ---

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getAdminLevelId() {
        return adminLevelId;
    }

    public void setAdminLevelId(Integer adminLevelId) {
        this.adminLevelId = adminLevelId;
    }

    // ★追加
    public UsersEntity getUser() {
        return user;
    }

    // ★追加
    public void setUser(UsersEntity user) {
        this.user = user;
    }
}