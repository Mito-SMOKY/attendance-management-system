package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;

@Repository
public interface EntryLogRepository extends JpaRepository<EntryLogEntity, Integer> {

    // 指定した教室IDと時間範囲で、未処理のログを取得するメソッド
    @Query("SELECT e FROM EntryLogEntity e " +
        "WHERE e.classroomId = :classroomId " +
        "AND e.entryTime BETWEEN :start AND :end " +
        "AND (e.isProcessed IS NULL OR e.isProcessed = 0)") 
    List<EntryLogEntity> findByClassroomIdAndEntryTimeBetween(
        @Param("classroomId") Integer classroomId, 
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end
    );
}