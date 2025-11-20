package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDate; // LocalDate をインポート

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "calendar")
public class CalendarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CalendarID")
    private Integer calendarId;

    @Column(name = "Title")
    private String title;

    @Column(name = "Date")
    private LocalDate date;

    // --- 関連定義 ---

    // Calendar(多) 対 Users(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID") // DBのFKカラム名
    private UsersEntity users;

    // --- コンストラクタ ---
    public CalendarEntity() {
    }

    // --- ゲッター・セッター ---
    public Integer getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(Integer calendarId) {
        this.calendarId = calendarId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    // 関連のゲッター・セッター
    public UsersEntity getUsers() {
        return users;
    }

    public void setUsers(UsersEntity users) {
        this.users = users;
    }
}