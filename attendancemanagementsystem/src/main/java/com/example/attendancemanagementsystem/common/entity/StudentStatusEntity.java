package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "studentstatus")
public class StudentStatusEntity {

    @Id
    @Column(name = "StudentStatusID")
    private Integer studentStatusId;

    @Column(name = "StudentStatusName")
    private String studentStatusName;

    public StudentStatusEntity() {
    }

    public Integer getStudentStatusId() {
        return studentStatusId;
    }

    public void setStudentStatusId(Integer studentStatusId) {
        this.studentStatusId = studentStatusId;
    }

    public String getStudentStatusName() {
        return studentStatusName;
    }

    public void setStudentStatusName(String studentStatusName) {
        this.studentStatusName = studentStatusName;
    }
}