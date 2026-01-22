package com.example.attendancemanagementsystem.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.DatalistDetailEntity;

@Repository
public interface DatalistDetailRepository extends JpaRepository<DatalistDetailEntity, Integer> {
    
    // ★変更: OrderByStudentNumberAsc → OrderByLoginIdAsc
    List<DatalistDetailEntity> findByDatalistIdOrderByLoginIdAsc(Integer datalistId);
}