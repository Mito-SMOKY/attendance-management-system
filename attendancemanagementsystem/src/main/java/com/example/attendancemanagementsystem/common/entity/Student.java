package com.example.attendancemanagementsystem.common.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "student")
public class Student {

    @Id
    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "StudentNumber", nullable = false, unique = true)
    private String studentNumber;

    @Column(name = "StudentStatusID", nullable = false)
    private Integer studentStatusId;

    @ManyToOne
    @JoinColumn(name = "DataListID", nullable = false)
    private Datalist datalist;

    @Column(name = "DeleteFlag")
    private boolean deleteFlag;

    // --- Getter / Setter ---

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public Integer getStudentStatusId() {
        return studentStatusId;
    }

    public void setStudentStatusId(Integer studentStatusId) {
        this.studentStatusId = studentStatusId;
    }

    public Datalist getDatalist() {
        return datalist;
    }

    public void setDatalist(Datalist datalist) {
        this.datalist = datalist;
    }

    public boolean isDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
    }
}