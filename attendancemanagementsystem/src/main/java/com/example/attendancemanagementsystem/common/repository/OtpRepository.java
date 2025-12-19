package com.example.attendancemanagementsystem.common.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.OtpEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;

@Repository
public interface OtpRepository extends JpaRepository<OtpEntity, Long> {
    // ユーザーに紐づくOTPデータを検索
    Optional<OtpEntity> findByUser(UsersEntity user);
}