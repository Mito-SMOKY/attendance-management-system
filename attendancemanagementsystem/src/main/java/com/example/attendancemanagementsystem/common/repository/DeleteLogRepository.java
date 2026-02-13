package com.example.attendancemanagementsystem.common.repository; // ←この行が重要

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.DeleteLogEntity;

@Repository
public interface DeleteLogRepository extends JpaRepository<DeleteLogEntity, Integer> {
}