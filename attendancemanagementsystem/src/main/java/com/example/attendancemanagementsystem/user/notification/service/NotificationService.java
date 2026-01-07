package com.example.attendancemanagementsystem.user.notification.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.NotificationEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.NotificationRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.common.service.FilterService;
import com.example.attendancemanagementsystem.common.service.SearchService;
import com.example.attendancemanagementsystem.user.notification.constant.NotificationType;
import com.example.attendancemanagementsystem.user.notification.dto.NotificationDto;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SearchService searchService;

    @Autowired
    private FilterService filterService;

    @Autowired
    private UsersRepository usersRepository;

    private static final Map<String, List<Integer>> TYPE_MAPPING = new HashMap<>();
    static {
        // グループコードごとのタイプIDリストを初期化
        for (NotificationType type : NotificationType.values()) {
            // グループコードごとにIDリストを追加
            TYPE_MAPPING
                .computeIfAbsent(type.getGroupCode(), k -> new ArrayList<>())
                .add(type.getId());
        }
    }

    // 検索機能
    public Map<String, Object> searchNotifications(UsersEntity user, int page, int size, String keyword, String type, String status, boolean bookmarkedOnly) {
        int pageIndex = (page > 0) ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by("createdAt").descending());

        // 1. フィルタ条件の構築
        Specification<NotificationEntity> spec = filterService.createEqualSpec("receiverUserId", user.getUserId());

        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and(searchService.createKeywordSpec(keyword, Arrays.asList("title", "message")));
        }

        spec = spec.and(filterService.createMappedInSpec(type, "notificationTypeId", TYPE_MAPPING));

        // ★注意：Entityのフィールド名が "isRead" か "read" かでここが変わります
        spec = spec.and(filterService.createBooleanStatusSpec(status, "read", "read", "unread"));

        if (bookmarkedOnly) {
            spec = spec.and(filterService.createEqualSpec("bookmarked", true));
        }

        // 2. 実行
        Page<NotificationEntity> pageResult = notificationRepository.findAll(spec, pageable);

        // 3. 整形 (convertToDtoにユーザー情報を渡すように改良)
        List<NotificationDto> content = pageResult.getContent().stream()
            .map(entity -> convertToDto(entity, user.getName())) // ログインユーザー名を渡す
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalPages", pageResult.getTotalPages());
        response.put("totalCount", pageResult.getTotalElements());
        
        return response;
    }

    // DTO変換
    private NotificationDto convertToDto(NotificationEntity entity, String userName) {
        return new NotificationDto(
            userName,
            entity.getNotificationId(), 
            entity.getTitle(), 
            entity.getMessage(), 
            entity.getCreatedAt(), 
            entity.isRead(), 
            entity.isBookmarked()
        );
    }

    public String getUserName(String loginId) {
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return (user.getName());
    }

    // 既読操作 
    @Transactional
    public boolean markAsRead(Integer notificationId, Integer userId) {
        return notificationRepository.findById(notificationId)
            .filter(n -> n.getReceiverUserId().equals(userId))
            .map(n -> { n.setRead(true); notificationRepository.save(n); return true; })
            .orElse(false);
    }

    // ブックマーク操作
    @Transactional
    public boolean toggleBookmark(Integer notificationId, Integer userId) {
        return notificationRepository.findById(notificationId)
            .filter(n -> n.getReceiverUserId().equals(userId))
            .map(n -> { n.setBookmarked(!n.isBookmarked()); notificationRepository.save(n); return true; })
            .orElse(false);
    }
    
    // 詳細取得＋既読化
    @Transactional
    public NotificationEntity getDetailAndMarkAsRead(Integer notificationId, Integer userId) {
        NotificationEntity notification = notificationRepository.findById(notificationId).orElse(null);
        // 自分宛て以外はnullを返す (セキュリティ)
        if (notification == null || !notification.getReceiverUserId().equals(userId)) return null;
        
        if (!notification.isRead()) { 
            notification.setRead(true); 
            notificationRepository.save(notification); 
        }
        return notification;
    }

    // 未読通知の有無確認
    public boolean hasUnreadNotifications(Integer userId) {
        return notificationRepository.countByReceiverUserIdAndIsReadFalse(userId) > 0;
    }
}