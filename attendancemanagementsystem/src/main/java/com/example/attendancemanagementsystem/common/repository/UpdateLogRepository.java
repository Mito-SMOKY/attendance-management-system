package com.example.attendancemanagementsystem.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.UpdateLogEntity;

@Repository
public interface UpdateLogRepository extends JpaRepository<UpdateLogEntity, Integer> {
}