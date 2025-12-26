package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        
        // DBから最新のユーザー情報を取得してパスワード照合
        UsersEntity user = usersRepository.findById(userDetails.getUserId()).orElseThrow();
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "パスワードが正しくありません。");
            return "redirect:/profile/security/email/auth";
        }

        // 認証成功フラグをセッションに保存
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

    // 4. OTP送信処理
    @PostMapping("/send-otp")
    public String sendOtpForEmailChange(
            @RequestParam("newEmail") String newEmail,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (usersRepository.findByEmail(newEmail).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "このメールアドレスは既に使用されています。");
            return "redirect:/profile/security/email/edit";
        }

        try {
            UsersEntity user = usersRepository.findById(userDetails.getUserId()).orElseThrow();
            otpService.sendOtp(user, newEmail, OtpPurpose.EMAIL_CHANGE);
            session.setAttribute("tempNewEmail", newEmail);
            return "redirect:/profile/security/email/verify";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "送信失敗: " + e.getMessage());
            return "redirect:/profile/security/email/edit";
        }
    }

    // 5. OTP入力画面
    @GetMapping("/verify")
    public String showEmailOtpPage(HttpSession session) {
        if (session.getAttribute("tempNewEmail") == null) {
            return "redirect:/profile/security/email/edit";
        }
        return "common/email_verify_otp";
    }

    // 6. OTP検証＆メール更新実行
    @PostMapping("/verify")
    public String verifyEmailOtp(
            @RequestParam("otp") String otp,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String newEmail = (String) session.getAttribute("tempNewEmail");
        if (newEmail == null) return "redirect:/profile/security/email/edit";

        UsersEntity user = usersRepository.findById(userDetails.getUserId()).orElseThrow();

        if (otpService.verifyOtp(user, otp, OtpPurpose.EMAIL_CHANGE)) {
            user.setEmail(newEmail);
            usersRepository.save(user);

            // ② 更新完了の通知とメールを送信します
            // user.getEmail() はすでに newEmail になっているため、変更先に送信されます
            try {
                // アプリ内通知 (ベルマーク)
                notificationMessageService.createEmailChangeCompletion(user);
                
                // 完了メール送信
                notificationEmailService.sendEmailChangeCompletion(user);
                
            } catch (Exception e) {
                e.printStackTrace(); // 通知失敗時も完了画面へ進めるようにする
            }

            // セッション情報のクリア
            session.removeAttribute("tempNewEmail");
            session.removeAttribute("emailChangeAuth");

            return "redirect:/profile/security/email/complete";
            
        } else {
            redirectAttributes.addFlashAttribute("error", "認証コードが正しくありません。");
            return "redirect:/profile/security/email/verify";
        }
    }

    // 7. 完了画面
    @GetMapping("/complete")
    public String showEmailCompletePage() {
        return "common/email_complete";
    }
}