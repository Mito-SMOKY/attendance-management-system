package com.example.attendancemanagementsystem.common.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.OtpEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.enums.OtpPurpose;
import com.example.attendancemanagementsystem.common.repository.OtpRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.notification.service.NotificationEmailService;
import com.example.attendancemanagementsystem.user.notification.service.NotificationMessageService;

@Service
public class OtpService {

    @Autowired private UsersRepository usersRepository;
    @Autowired private OtpRepository otpRepository;
    @Autowired private NotificationMessageService notificationMessageService;
    @Autowired private NotificationEmailService notificationEmailService;
    @Autowired private PasswordEncoder passwordEncoder;

    // 設定値
    private static final int EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_MINUTES = 1;
    private static final SecureRandom secureRandom = new SecureRandom();

    // OTP送信処理
    @Transactional
    public void sendOtp(UsersEntity user, String targetEmail, OtpPurpose purpose) {
        
        OtpEntity otpEntity = otpRepository.findByUser(user)
                .orElse(new OtpEntity(user));

        // 再送制限（連打防止）
        if (otpEntity.getCreateAt() != null) {
            LocalDateTime unlockTime = otpEntity.getCreateAt().plusMinutes(RESEND_COOLDOWN_MINUTES);
            if (LocalDateTime.now().isBefore(unlockTime)) {
                throw new RuntimeException("再送するには少し時間を空けてください。");
            }
        }

        // 生のOTP生成 (6桁)
        String rawToken = String.valueOf(secureRandom.nextInt(900000) + 100000);

        // ハッシュ化して保存
        otpEntity.setToken(passwordEncoder.encode(rawToken)); 
        
        otpEntity.setPurpose(purpose);
        otpEntity.setExpiredAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        otpEntity.setCreateAt(LocalDateTime.now());
        otpEntity.setAttempts(0);

        otpRepository.save(otpEntity);

        // 通知送信（メール & サイト内）
        if (purpose == OtpPurpose.PASSWORD_RESET) {
            notificationEmailService.sendPasswordResetOtp(user, rawToken, EXPIRY_MINUTES);
        
        } else if (purpose == OtpPurpose.EMAIL_CHANGE) {
            notificationEmailService.sendEmailChangeOtp(user, targetEmail, rawToken, EXPIRY_MINUTES);
        }
    }

    // OTP検証処理
    @Transactional
    public boolean verifyOtp(UsersEntity user, String inputCode, OtpPurpose purpose) {
        OtpEntity otpEntity = otpRepository.findByUser(user).orElse(null);

        // 基本チェック
        if (otpEntity == null || otpEntity.getToken() == null) return false;
        if (otpEntity.getExpiredAt().isBefore(LocalDateTime.now())) return false;
        if (otpEntity.getAttempts() >= MAX_ATTEMPTS) return false;

        // 目的チェック
        if (otpEntity.getPurpose() != purpose) {
            return false;
        }

        // トークン照合
        if (!passwordEncoder.matches(inputCode, otpEntity.getToken())) {
            
            // 間違いカウントアップ
            otpEntity.setAttempts(otpEntity.getAttempts() + 1);
            
            // 上限超えたら無効化
            if (otpEntity.getAttempts() >= MAX_ATTEMPTS) {
                otpEntity.setToken(null); 
                otpEntity.setExpiredAt(null);
            }
            otpRepository.save(otpEntity);
            return false;
        }

        return true;
    }
    
    //  パスワード更新処理（OTP検証後に呼び出す）
    @Transactional
    public void updatePassword(String email, String newRawPassword) {
        UsersEntity user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

        // パスワード更新
        user.setPassword(passwordEncoder.encode(newRawPassword));
        usersRepository.save(user);

        // トークン掃除
        clearOtp(user);

        // 完了通知 (メール & サイト内)]
        try {
            notificationEmailService.sendPasswordChangeCompletion(user);
        } catch (Exception e) {
            System.err.println("完了メール送信失敗（処理は続行）: " + e.getMessage());
        }
        
        try {
            notificationMessageService.createPasswordChangeCompletion(user);
        } catch (Exception e) {
            System.err.println("完了通知作成失敗（処理は続行）: " + e.getMessage());
        }
    }

    // OTP情報をクリアする
    @Transactional
    public void clearOtp(UsersEntity user) {
        otpRepository.findByUser(user).ifPresent(otp -> {
            otp.setToken(null);
            otp.setPurpose(null);
            otp.setExpiredAt(null);
            otp.setAttempts(0);
            otp.setCreateAt(null);
            otpRepository.save(otp);
        });
    }
}