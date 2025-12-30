package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;

@Repository
public interface EntryLogRepository extends JpaRepository<EntryLogEntity, Integer> {
    // 指定した教室・時間帯のログを取得
    List<EntryLogEntity> findByClassroomIdAndEntryTimeBetween(
        Integer classroomId, 
        LocalDateTime start, 
        LocalDateTime end
    );
}