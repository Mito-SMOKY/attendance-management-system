package com.example.attendancemanagementsystem.common.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlotEntity, Integer> {

    // 全時限を時限ID順で取得
    List<TimeSlotEntity> findAllByOrderBySlotIdAsc();
}