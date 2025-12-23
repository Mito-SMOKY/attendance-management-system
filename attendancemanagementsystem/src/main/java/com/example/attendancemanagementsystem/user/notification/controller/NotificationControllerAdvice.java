package com.example.attendancemanagementsystem.user.notification.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.notification.service.NotificationService;

@ControllerAdvice
public class NotificationControllerAdvice {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UsersRepository usersRepository;

    // 未読通知の有無をモデルに追加
    @ModelAttribute("hasUnread")
    public boolean addUnreadStatus(@AuthenticationPrincipal UserDetails userDetails) {
        // ログインしていない場合は赤丸なし
        if (userDetails == null) {
            return false;
        }

        // ユーザー情報を取得
        UsersEntity user = usersRepository.findByLoginId(userDetails.getUsername()).orElse(null);
        
        if (user != null) {
            // 未読通知の有無を取得
            return notificationService.hasUnreadNotifications(user.getUserId());
        }
        
        return false;
    }
}