package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.enums.OtpPurpose;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.common.service.OtpService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;
import com.example.attendancemanagementsystem.user.notification.service.NotificationEmailService;
import com.example.attendancemanagementsystem.user.notification.service.NotificationMessageService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/profile/security/email") 
public class ProfileEmailController {

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private OtpService otpService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private NotificationMessageService notificationMessageService;
    @Autowired
    private NotificationEmailService notificationEmailService;

    // 1. 本人確認（パスワード入力）画面
    @GetMapping("/auth")
    public String showEmailAuthPage() {
        return "common/email_auth";
    }

    // 2. パスワード検証処理
    @PostMapping("/auth")
    public String verifyPasswordForEmailChange(
            @RequestParam("password") String password,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        UsersEntity user = usersRepository.findById(userDetails.getUserId()).orElseThrow();
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "パスワードが正しくありません。");
            return "redirect:/profile/security/email/auth";
        }

        session.setAttribute("emailChangeAuth", true);
        return "redirect:/profile/security/email/edit";
    }

    // 3. 新メールアドレス入力画面
    @GetMapping("/edit")
    public String showEmailEditPage(HttpSession session) {
        if (session.getAttribute("emailChangeAuth") == null) {
            return "redirect:/profile/security/email/auth";
        }
        return "common/reset_email";
    }

    // 4. OTP送信処理 (初回)
    @PostMapping("/send-otp")
    public String sendOtpForEmailChange(
            @RequestParam("newEmail") String newEmail,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // 既存チェック
        if (usersRepository.findByEmail(newEmail).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "このメールアドレスは既に使用されています。");
            return "redirect:/profile/security/email/edit";
        }

        try {
            UsersEntity user = usersRepository.findById(userDetails.getUserId()).orElseThrow();
            otpService.sendOtp(user, newEmail, OtpPurpose.EMAIL_CHANGE);
            
            // セッションに保存
            session.setAttribute("tempNewEmail", newEmail);
            
            // 入力画面へ
            return "redirect:/profile/security/email/verify-otp";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "送信失敗: " + e.getMessage());
            return "redirect:/profile/security/email/edit";
        }
    }

    // ★追加: 4.5 OTP再送信処理
    @PostMapping("/resend-otp")
    public String resendOtp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        // セッションからメールアドレスを取り出す
        String newEmail = (String) session.getAttribute("tempNewEmail");
        if (newEmail == null) {
            // セッション切れなら最初に戻す
            return "redirect:/profile/security/email/edit";
        }

        UsersEntity user = usersRepository.findById(userDetails.getUserId()).orElseThrow();
        
        try {
            otpService.sendOtp(user, newEmail, OtpPurpose.EMAIL_CHANGE);
            // JSのタイマーを動かすためのフラグ
            redirectAttributes.addFlashAttribute("message", "sent");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "too_soon");
        }
        
        return "redirect:/profile/security/email/verify-otp";
    }

    // 5. OTP入力画面
    // URLを verify-otp に統一
    @GetMapping("/verify-otp")
    public String showEmailOtpPage(HttpSession session) {
        if (session.getAttribute("tempNewEmail") == null) {
            return "redirect:/profile/security/email/edit";
        }
        return "common/email_verify_otp";
    }

    // 6. OTP検証＆メール更新実行 (JSON対応)
    // URLを verify-otp に統一
    @PostMapping("/verify-otp")
    @ResponseBody // HTMLではなくデータを返す
    public Map<String, Object> verifyEmailOtp(
            @RequestParam("otp") String otp,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        String newEmail = (String) session.getAttribute("tempNewEmail");
        if (newEmail == null) {
            response.put("success", true); // リダイレクトさせるためtrue扱い
            response.put("redirectUrl", "/profile/security/email/edit");
            return response;
        }

        UsersEntity user = usersRepository.findById(userDetails.getUserId()).orElseThrow();

        if (otpService.verifyOtp(user, otp, OtpPurpose.EMAIL_CHANGE)) {
            // 更新実行
            user.setEmail(newEmail);
            usersRepository.save(user);
            otpService.clearOtp(user);
            
            // 通知
            try {
                notificationMessageService.createEmailChangeCompletion(user);
                notificationEmailService.sendEmailChangeCompletion(user);
            } catch (Exception e) {
                e.printStackTrace(); 
            }

            session.removeAttribute("tempNewEmail");
            session.removeAttribute("emailChangeAuth");

            // 成功時のリダイレクト先をJSONで返す
            response.put("success", true);
            response.put("redirectUrl", "/profile/security/email/complete");
            
        } else {
            // 失敗時
            response.put("success", false);
            response.put("message", "invalid");
        }
        
        return response;
    }

    // 7. 完了画面
    @GetMapping("/complete")
    public String showEmailCompletePage() {
        return "common/email_complete";
    }
}