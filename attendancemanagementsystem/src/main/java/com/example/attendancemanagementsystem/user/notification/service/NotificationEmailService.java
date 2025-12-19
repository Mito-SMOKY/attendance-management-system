package com.example.attendancemanagementsystem.user.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.attendancemanagementsystem.attendance.display.dto.SubjectListDto;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.service.EmailService;
import com.example.attendancemanagementsystem.user.notification.constant.NotificationType;

@Service
public class NotificationEmailService {

    @Autowired
    private EmailService emailService;

    // ワンタイムパスワードの送信(otp)
    public void sendPasswordResetOtp(UsersEntity user, String otp, int expiryMinutes) {
        send(user, NotificationType.PASSWORD_RESET_OTP, null, 
            user.getName(), otp, expiryMinutes);
    }

    // パスワード変更完了通知送信
    public void sendPasswordChangeCompletion(UsersEntity user) {
        send(user, NotificationType.PASSWORD_CHANGE_COMPLETED, null, 
            user.getName());
    }

    // 出席率アラート送信
    public void sendAttendanceRisk(UsersEntity student, SubjectListDto dto) {
        String subjectName = dto.getSubjectName();
        int rate = (int)(dto.getAttendanceRate() * 100);
        
        send(student, NotificationType.ATTENDANCE_RISK_ALERT, subjectName, 
            student.getName(), subjectName, rate);
    }

    // メールアドレス変更確認コード送信
    public void sendEmailChangeOtp(UsersEntity user, String targetEmail, String otp, int expiryMinutes) {
        sendToAddress(targetEmail, NotificationType.EMAIL_CHANGE_OTP, null, 
            user.getName(), otp, expiryMinutes);
    }

    // メールアドレス変更完了通知送信
    public void sendEmailChangeCompletion(UsersEntity user) {
        send(user, NotificationType.EMAIL_CHANGE_COMPLETED, null, 
            user.getName());
    }

     // 指定アドレスへ送信処理
    private void sendToAddress(String toAddress, NotificationType type, String subjectArg, Object... bodyArgs) {
        try {
            String subject = type.getSubjectTemplate();
            if (subject.contains("%s") && subjectArg != null) {
                subject = String.format(subject, subjectArg);
            }

            String body = String.format(type.getBodyTemplate(), bodyArgs);
            emailService.sendEmail(toAddress, subject, body);
            
        } catch (Exception e) {
            System.err.println("メール送信エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ユーザーの登録メールアドレスへ送信処理
    private void send(UsersEntity user, NotificationType type, String subjectArg, Object... bodyArgs) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) return;
        sendToAddress(user.getEmail(), type, subjectArg, bodyArgs);
    }
}