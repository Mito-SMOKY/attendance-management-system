package com.example.attendancemanagementsystem.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.AttendanceStatusEntity;

@Repository
public interface AttendanceStatusRepository extends JpaRepository<AttendanceStatusEntity, Integer> {

    // ステータス名にもとづく出席ステータスの取得
    AttendanceStatusEntity findByStatusName(String statusName);
}