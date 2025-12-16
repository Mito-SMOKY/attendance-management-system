package com.example.attendancemanagementsystem.user.loginandprofile.service;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.notification.service.NotificationEmailService;
import com.example.attendancemanagementsystem.user.notification.service.NotificationMessageService;

@Service
public class PasswordResetService {

    @Autowired private UsersRepository usersRepository;
    @Autowired private NotificationMessageService notificationMessageService;
    @Autowired private NotificationEmailService notificationEmailService;
    @Autowired private PasswordEncoder passwordEncoder;

    // パスワードリセット用OTP送信
    @Transactional
    public String sendVerificationCode(String email) {
        
        // ユーザー検索
        UsersEntity user = usersRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return null;
        }

        // OTP生成
        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(1000000));
        
        int expiryMinutes = 30; // 有効期限30分

        // 通知送信
        notificationMessageService.createPasswordResetOtp(user, otp, expiryMinutes);

        // メール送信
        notificationEmailService.sendPasswordResetOtp(user, otp, expiryMinutes);
        
        return otp;
    }

    // パスワード更新処理
    @Transactional
    public void updatePassword(String email, String newRawPassword) {
        // ユーザーを取得
        UsersEntity user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // パスワードを暗号化
        String encodedPassword = passwordEncoder.encode(newRawPassword);

        // DB更新
        user.setPassword(encodedPassword);
        usersRepository.save(user);

        // 通知送信
        notificationMessageService.createPasswordChangeCompletion(user);
        
        // メール送信
        notificationEmailService.sendPasswordChangeCompletion(user);    
    }
}