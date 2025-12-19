package com.example.attendancemanagementsystem.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // application.properties に書いた "spring.mail.username" を自動で読み込む
    @Value("${spring.mail.username}")
    private String fromAddress;

    /**
     * 汎用的なメール送信メソッド
     * @param to 送信先アドレス
     * @param subject 件名
     * @param body 本文
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress); // 設定ファイルのアドレスから送信
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            System.out.println("メール送信成功: To=" + to);
        } catch (Exception e) {
            System.err.println("メール送信失敗: " + e.getMessage());
            // 必要であればログ出力や例外のスローを行う
        }
    }
}