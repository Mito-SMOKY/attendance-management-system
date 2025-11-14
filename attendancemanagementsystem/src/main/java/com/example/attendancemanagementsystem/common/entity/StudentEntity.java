package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "student")
public class StudentEntity {

    @Id
    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "StudentStatusID", nullable = false)
    private Integer studentStatusId;

    @Column(name = "DataListID", nullable = false)
    private Integer dataListId;

    @Column(name = "CreatedAt", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "DeleteFlag", nullable = false)
    private boolean deleteFlag;

   // --- Getter ---
    public Integer getUserId() {
        return userId;
    }

    public Integer getStudentStatusId() {
        return studentStatusId;
    }

    public Integer getDataListId() {
        return dataListId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isDeleteFlag() {
        return deleteFlag;
    }

    // --- Setter ---
    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setStudentStatusId(Integer studentStatusId) {
        this.studentStatusId = studentStatusId;
    }

    public void setDataListId(Integer dataListId) {
        this.dataListId = dataListId;
    }

    // CreatedAt はDB自動生成のためSetterは不要（ここでは省略）

    public void setDeleteFlag(boolean deleteFlag) {
        this.deleteFlag = deleteFlag;
    }


}