package com.example.attendancemanagementsystem.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.NotificationEntity;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Integer> {
    
    // 特定のユーザーの通知を取得（作成日時の新しい順）
    List<NotificationEntity> findByReceiverUserIdOrderByCreatedAtDesc(Integer receiverUserId);
}
