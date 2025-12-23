package com.example.attendancemanagementsystem.user.notification.service;

import java.util.ArrayList;
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
        
        // ページングとソート設定
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        // 自分通知のみ
        Specification<NotificationEntity> spec = filterService.createEqualSpec("receiverUserId", user.getUserId());

        // キーワード検索
        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and(searchService.createKeywordSpec(keyword, java.util.Arrays.asList("message")));
        }

        // タイプフィルタ
        spec = spec.and(filterService.createMappedInSpec(type, "notificationTypeId", TYPE_MAPPING));

        // ステータスフィルタ
        spec = spec.and(filterService.createBooleanStatusSpec(status, "isRead", "read", "unread"));

        // ブックマークフィルタ
        if (bookmarkedOnly) {
            spec = spec.and(filterService.createEqualSpec("isBookmarked", true));
        }

        // 実行
        Page<NotificationEntity> pageResult = notificationRepository.findAll(spec, pageable);

        //整形
        List<NotificationDto> content = pageResult.getContent().stream().map(this::convertToDto).collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalPages", pageResult.getTotalPages());
        response.put("totalCount", pageResult.getTotalElements());
        return response;
    }

    // 通知タイトル解決
    public String resolveTitle(Integer typeId) {
        for (NotificationType nt : NotificationType.values()) {
            if (nt.getId() == typeId) return nt.getSubjectTemplate();
        }
        return "お知らせ";
    }

    // DTO変換
    private NotificationDto convertToDto(NotificationEntity entity) {
        String title = resolveTitle(entity.getNotificationTypeId());
        return new NotificationDto(entity.getNotificationId(), title, entity.getMessage(), entity.getCreatedAt(), entity.isRead(), entity.isBookmarked());
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
        if (notification == null || !notification.getReceiverUserId().equals(userId)) return null;
        if (!notification.isRead()) { notification.setRead(true); notificationRepository.save(notification); }
        return notification;
    }

    // 未読通知の有無確認
    public boolean hasUnreadNotifications(Integer userId) {
    return notificationRepository.countByReceiverUserIdAndIsReadFalse(userId) > 0;
}
}