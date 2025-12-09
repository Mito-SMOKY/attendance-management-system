package com.example.attendancemanagementsystem.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.AdministratorEntity;

@Repository
public interface AdministratorRepository extends JpaRepository<AdministratorEntity, Integer> {
    // UserIDを主キーとしているため、標準の findById で検索可能です
}
