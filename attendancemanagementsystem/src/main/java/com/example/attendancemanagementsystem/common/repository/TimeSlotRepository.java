package com.example.attendancemanagementsystem.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlotEntity, Integer> {
    List<TimeSlotEntity> findAllByOrderBySlotIdAsc();
}