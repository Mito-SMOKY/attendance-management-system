package com.example.attendancemanagementsystem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendancemanagementsystem.Dummymodel.NotificationDto;
import com.example.attendancemanagementsystem.Dummymodel.NotificationService;
import com.example.attendancemanagementsystem.Dummymodel.PageResponse;

/**
 * 全ロール共通のAPIエンドポイント
 */
@RestController
@RequestMapping("/api") // ロール名を含まない共通パス
public class Dummyapi {

    private final NotificationService notificationService;

    @Autowired
    public Dummyapi (NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 通知一覧取得API
     * GET /api/notifications
     */
    @GetMapping("/notifications")
    public PageResponse<NotificationDto> getNotifications(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean bookmarked,
            @RequestParam(defaultValue = "1") int page, 
            @RequestParam(defaultValue = "10") int size 
    ) {
        // Serviceに引数を渡して、ページネーションされた結果を受け取って返す
        return notificationService.findNotifications(keyword, type, status, bookmarked, page, size);
    }

    @PostMapping("/notifications/{id}/bookmark")
    public void toggleBookmark(@PathVariable Long id) {
        notificationService.toggleBookmark(id);
    }
}