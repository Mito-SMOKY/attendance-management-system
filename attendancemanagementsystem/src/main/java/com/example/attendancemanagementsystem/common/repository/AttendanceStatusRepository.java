package com.example.attendancemanagementsystem.common.repository;

import com.example.attendancemanagementsystem.common.entity.AttendanceStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceStatusRepository extends JpaRepository<AttendanceStatusEntity, Integer> {
}