package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "classroom")
public class ClassroomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ClassroomID")
    private Integer classroomId;

    @Column(name = "ClassroomName", nullable = false)
    private String classroomName;

    @Column(name = "MacAddress")
    private String macAddress;

    // --- constructor ---
    public ClassroomEntity() {}

    // --- Getter/Setter ---
    public Integer getClassroomId() { return classroomId; }
    public void setClassroomId(Integer classroomId) { this.classroomId = classroomId; }

    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
}