package com.example.attendancemanagementsystem.common.entity;

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
@Table(name = "entrylog")
public class EntryLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EntryLogID")
    private Integer entryLogId;

    @Column(name = "UserID")
    private Integer userId;

    @Column(name = "ClassroomID")
    private Integer classroomId;

    @Column(name = "EntryTime")
    private LocalDateTime entryTime;

    @Column(name = "IsProcessed")
    private Integer isProcessed = 0; 

    @Column(name = "ProcessedAt")
    private LocalDateTime processedAt;
}
