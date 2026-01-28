package com.example.attendancemanagementsystem.common.entity;



import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
@Entity
@Table(name = "student")
public class Student {

    @Id
    @Column(name = "UserID")
    private Integer userId; // UsersテーブルのIDと同じ

    //private String studentNumber; 

    @Column(name = "StudentStatusID", nullable = false)
    private Integer studentStatusId;

    @ManyToOne
    @JoinColumn(name = "DataListID", nullable = false)
    @JsonIgnore
    private Datalist datalist;

    //Usersテーブルと紐づける（IDを共有するため OneToOne @MapsId が理想ですが、簡易的にマッピング）
    @OneToOne(cascade = CascadeType.ALL)
    @MapsId // StudentのIDはUsersのIDと同じものを使う設定
    @JoinColumn(name = "UserID") 
    private UsersEntity user;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", insertable = false, updatable = false)
    private List<EnrollmentsEntity> enrollments;

    // --- Getter / Setter ---
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getStudentStatusId() { return studentStatusId; }
    public void setStudentStatusId(Integer studentStatusId) { this.studentStatusId = studentStatusId; }

    public Datalist getDatalist() { return datalist; }
    public void setDatalist(Datalist datalist) { this.datalist = datalist; }

    public UsersEntity getUser() { return user; }
    public void setUser(UsersEntity user) { this.user = user; }

    // ★追加分のGetter/Setter
    public List<EnrollmentsEntity> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<EnrollmentsEntity> enrollments) {
        this.enrollments = enrollments;
    }
}
