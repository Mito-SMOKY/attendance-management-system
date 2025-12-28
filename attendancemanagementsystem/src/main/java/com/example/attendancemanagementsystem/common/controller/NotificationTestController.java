package com.example.attendancemanagementsystem.common.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.notification.service.NotificationMessageService;

@RestController
@RequestMapping("/test")
public class NotificationTestController {

    @Autowired
    private NotificationMessageService notificationMessageService;

    @Autowired
    private UsersRepository usersRepository;

    /**
     * テスト用: 指定したユーザーIDに「パスワード変更完了」通知（ID:2）を強制送信する
     * URL例: http://localhost:8080/test/notify?userId=2521006
     */
    @GetMapping("/notify")
    public String sendTestNotification(@RequestParam("userId") Integer userId) {
        
        // 1. ユーザーをDBから探す
        UsersEntity student = usersRepository.findById(userId).orElse(null);
        
        if (student == null) {
            return "エラー: UserID " + userId + " が見つかりません。";
        }

        try {
            // 2. 既存の通知サービスを使って通知を作成・保存
            // (引数が一番少ない createPasswordChangeCompletion をテスト用に利用します)
            notificationMessageService.createPasswordChangeCompletion(student);
            
            return "成功: " + student.getName() + " (ID:" + userId + ") に通知を送信しました。";
            
        } catch (Exception e) {
            e.printStackTrace();
            return "失敗: エラーが発生しました -> " + e.getMessage();
        }
    }
}