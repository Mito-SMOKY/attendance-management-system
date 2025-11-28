package com.example.attendancemanagementsystem.common.entity;



import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "student")
public class Student {

    @Id
    @Column(name = "UserID")
    private Integer userId; // UsersテーブルのIDと同じ

    // ★削除: private String studentNumber; 

    @Column(name = "StudentStatusID", nullable = false)
    private Integer studentStatusId;

    @ManyToOne
    @JoinColumn(name = "DataListID", nullable = false)
    @JsonIgnore
    private Datalist datalist;

    @Column(name = "DeleteFlag")
    private boolean deleteFlag;

    // ★追加: Usersテーブルと紐づける（IDを共有するため OneToOne @MapsId が理想ですが、簡易的にマッピング）
    @OneToOne(cascade = CascadeType.ALL)
    @MapsId // StudentのIDはUsersのIDと同じものを使う設定
    @JoinColumn(name = "UserID") 
    private UsersEntity user;

    // --- Getter / Setter ---
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    // getStudentNumber / setStudentNumber は削除

    public Integer getStudentStatusId() { return studentStatusId; }
    public void setStudentStatusId(Integer studentStatusId) { this.studentStatusId = studentStatusId; }

    public Datalist getDatalist() { return datalist; }
    public void setDatalist(Datalist datalist) { this.datalist = datalist; }

    public boolean isDeleteFlag() { return deleteFlag; }
    public void setDeleteFlag(boolean deleteFlag) { this.deleteFlag = deleteFlag; }

    public UsersEntity getUser() { return user; }
    public void setUser(UsersEntity user) { this.user = user; }
}
