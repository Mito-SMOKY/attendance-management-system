package com.example.attendancemanagementsystem.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.AdminHistoryEntity;

@Repository
public interface AdminHistoryRepository extends JpaRepository<AdminHistoryEntity, String> {
    // 特に追加メソッドなしで、findAll(Pageable)が使えます
}