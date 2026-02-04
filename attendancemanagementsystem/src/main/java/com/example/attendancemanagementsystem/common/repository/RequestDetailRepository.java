package com.example.attendancemanagementsystem.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.RequestDetailEntity;

@Repository
public interface RequestDetailRepository extends JpaRepository<RequestDetailEntity, Integer> {
    // RequestIDでRequestDetailを取得
    List<RequestDetailEntity> findByRequest_RequestId(Integer requestId);
}