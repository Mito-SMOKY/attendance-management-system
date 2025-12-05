package com.example.attendancemanagementsystem.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Classroom")
public class ClassroomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ClassroomID")
    private Integer classroomId;

    @Column(name = "ClassroomName")
    private String classroomName;

    @Column(name = "MacAddress")
    private String macAddress;

    // --- Getter / Setter ---
    public Integer getClassroomId() { 
        return classroomId; 
    }

    public void setClassroomId(Integer classroomId) { 
        this.classroomId = classroomId;
    }

    public String getClassroomName() { 
        return classroomName;
    }

    public void setClassroomName(String classroomName) { 
        this.classroomName = classroomName; 
    }

    public String getMacAddress() { 
        return macAddress; 
    }

    public void setMacAddress(String macAddress) { 
        this.macAddress = macAddress; 
    }
}