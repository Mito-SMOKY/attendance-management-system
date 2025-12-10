package com.example.attendancemanagementsystem.test.mail.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class TestMailService {

    @Autowired
    private JavaMailSender mailSender;

    // application.properties の設定値を確認するためにここでも読み込む
    @Value("${spring.mail.username}")
    private String fromAddress;

    public void sendTestEmail(String toAddress, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toAddress);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            System.out.println("【テスト】メール送信成功: " + toAddress);
            
        } catch (Exception e) {
            System.err.println("【テスト】メール送信失敗: " + e.getMessage());
            throw new RuntimeException("送信エラー: " + e.getMessage());
        }
    }
}