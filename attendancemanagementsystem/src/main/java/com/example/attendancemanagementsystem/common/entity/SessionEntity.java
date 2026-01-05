package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "classsession")
public class SessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SessionID")
    private Integer sessionId;

    @Column(name = "TimeTableID")
    private Integer timeTableId;

    @Column(name = "ActualClassroomID")
    private Integer actualClassroomId;

    @Column(name = "SessionDate")
    private LocalDate sessionDate;

    @Column(name = "StartTime")
    private LocalDateTime startTime;

    @Column(name = "EndTime")
    private LocalDateTime endTime;

    @Column(name = "SessionStatus", columnDefinition = "TINYINT")
    private Integer sessionStatus;

    @Column(name = "Note")
    private String note;
}