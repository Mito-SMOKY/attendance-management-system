package com.example.attendancemanagementsystem.common.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // 1. 追加
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.NotificationEntity;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Integer>, JpaSpecificationExecutor<NotificationEntity> { // 2. 追加

    // 受信者ユーザーIDで通知を取得
    List<NotificationEntity> findByReceiverUserId(Integer receiverUserId);

    // 受信者ユーザーIDで通知を取得し、作成日時で降順に並べ替え、ページング対応
    Page<NotificationEntity> findByReceiverUserIdOrderByCreatedAtDesc(Integer receiverUserId, Pageable pageable);

    // 未読の数を数えるメソッド 
    long countByReceiverUserIdAndIsReadFalse(Integer receiverUserId);
}