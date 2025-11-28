package com.example.attendancemanagementsystem.common.entity;


import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "datalist")
public class Datalist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DataListID")
    private Integer dataListId;

    @Column(name = "DataListName", nullable = false)
    private String dataListName;

    @Column(name = "Creator", nullable = false)
    private Integer creatorId;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "datalist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Student> students;

    @ManyToOne
    @JoinColumn(name = "Creator", insertable = false, updatable = false)
    private UsersEntity creatorUser;

    // --- Getter / Setter ---

    public Integer getDataListId() {
        return dataListId;
    }

    public void setDataListId(Integer dataListId) {
        this.dataListId = dataListId;
    }

    public String getDataListName() {
        return dataListName;
    }

    public void setDataListName(String dataListName) {
        this.dataListName = dataListName;
    }

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }

    public UsersEntity getCreatorUser() {
        return creatorUser;
    }

    public void setCreatorUser(UsersEntity creatorUser) {
        this.creatorUser = creatorUser;
    }

    // 必要に応じて toString() なども追加
    @Override
    public String toString() {
        return "Datalist{" +
                "dataListId=" + dataListId +
                ", dataListName='" + dataListName + '\'' +
                ", creatorId=" + creatorId +
                ", createdAt=" + createdAt +
                '}';
    }
}