package com.example.attendancemanagementsystem.user.notification.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.common.entity.NotificationEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.notification.service.NotificationService;

@Controller
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UsersRepository usersRepository;

    // 通知一覧画面
    @GetMapping("/notifications")
    public String viewNotificationList(Model model) {
        return "common/notification"; 
    }

    // 通知詳細画面 
    @GetMapping("/notification/detail")
    public String viewNotificationDetail(
            @RequestParam("id") Integer id, 
            Model model, 
            @AuthenticationPrincipal UserDetails userDetails) { // Authenticationは不要なので整理
        
        if (userDetails == null) return "redirect:/login";

        // 1. ログインユーザー情報を取得
        String loginId = userDetails.getUsername();
        UsersEntity user = usersRepository.findByLoginId(loginId).orElse(null);
        if (user == null) return "redirect:/login";

        // 2. 通知詳細を取得し既読にする
        NotificationEntity notification = notificationService.getDetailAndMarkAsRead(id, user.getUserId());
        if (notification == null) {
            return "redirect:/notifications"; 
        }

        // 3. 【重要】HTMLとの整合性をとる
        // HTMLで ${userData.userName} と書く場合、userData というキーで 
        // getUserName() メソッドを持つオブジェクト（UserEntity）を渡します。
        model.addAttribute("userData", user); 
        
        // 通知データ本体
        model.addAttribute("notification", notification);
        model.addAttribute("title", notification.getTitle());
        
        return "common/notificationDetail";
    }

    // API: 通知一覧取得
    @GetMapping("/api/notifications")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotifications(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "bookmarked", required = false) boolean bookmarkedOnly,
            Authentication authentication) {
        
        UsersEntity user = getCurrentUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        Map<String, Object> response = notificationService.searchNotifications(user, page, size, keyword, type, status, bookmarkedOnly);

        return ResponseEntity.ok(response);
    }

    // API: 既読化
    @PostMapping("/api/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<Void> markAsRead(@PathVariable Integer id, Authentication authentication) {
        UsersEntity user = getCurrentUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        boolean success = notificationService.markAsRead(id, user.getUserId());
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // API: ブックマーク切替
    @PostMapping("/api/notifications/{id}/bookmark")
    @ResponseBody
    public ResponseEntity<Void> toggleBookmark(@PathVariable Integer id, Authentication authentication) {
        UsersEntity user = getCurrentUser(authentication);
        if (user == null) return ResponseEntity.status(401).build();

        boolean success = notificationService.toggleBookmark(id, user.getUserId());
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    private UsersEntity getCurrentUser(Authentication authentication) {
        if (authentication == null) return null;
        return usersRepository.findByLoginId(authentication.getName()).orElse(null);
    }
}