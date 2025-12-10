package com.example.attendancemanagementsystem.test.mail.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.test.mail.service.TestMailService;

@Controller
@RequestMapping("/test/mail") // URLは /test/mail/...
public class TestMailController {

    @Autowired
    private TestMailService testMailService;

    // 1. テスト画面表示
    @GetMapping("")
    public String showPage() {
        return "test/mail/index"; // templates/test/mail/index.html を表示
    }

    // 2. 送信実行
    @PostMapping("/send")
    public String sendMail(
            @RequestParam("to") String to,
            @RequestParam("subject") String subject,
            @RequestParam("body") String body,
            Model model) {

        try {
            testMailService.sendTestEmail(to, subject, body);
            model.addAttribute("successMessage", "送信に成功しました！ (" + to + ")");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "送信に失敗しました: " + e.getMessage());
        }

        return "test/mail/index";
    }
}