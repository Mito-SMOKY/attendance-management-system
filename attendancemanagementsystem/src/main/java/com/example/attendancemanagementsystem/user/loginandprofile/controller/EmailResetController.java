package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.enums.OtpPurpose;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.common.service.OtpService;
import com.example.attendancemanagementsystem.user.notification.service.NotificationEmailService;
import com.example.attendancemanagementsystem.user.notification.service.NotificationMessageService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/email")
public class EmailResetController {

    @Autowired
    private OtpService otpService;
    
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificationEmailService notificationEmailService;

    @Autowired
    private NotificationMessageService notificationMessageService;

    // 本人確認画面
    @GetMapping("/auth")
    public String showAuthForm() {
        return "login/email_auth";
    }

    // 本人確認処理
    @PostMapping("/auth")
    public String processAuth(
            @RequestParam("loginId") String loginId,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {
        
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "IDまたはパスワードが間違っています");
            return "login/email_auth";
        }

        session.setAttribute("resetEmail_UserId", user.getUserId());
        return "redirect:/email/reset-email";
    }

    // 新メールアドレス入力画面
    @GetMapping("/reset-email")
    public String showResetEmailForm(HttpSession session) {
        if (session.getAttribute("resetEmail_UserId") == null) {
            return "redirect:/email/auth";
        }
        return "login/reset_email";
    }

    // OTP送信処理（初回送信 ＆ 再送信ボタンもここ宛て）
    @PostMapping("/send-otp")
    public String sendOtp(
            @RequestParam("newEmail") String newEmail,
            @RequestParam(name = "isResend", required = false, defaultValue = "false") boolean isResend,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        Integer userId = (Integer) session.getAttribute("resetEmail_UserId");
        if (userId == null) return "redirect:/email/auth";

        // 初回送信時のみ重複チェックを行う
        if (!isResend && usersRepository.findByEmail(newEmail).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "このメールアドレスは既に使用されています");
            return "redirect:/email/reset-email";
        }

        try {
            UsersEntity user = usersRepository.findById(userId).orElseThrow();
            
            // ★ OtpServiceに委譲 (DB保存, メール送信, クールタイムチェック)
            otpService.sendOtp(user, newEmail, OtpPurpose.EMAIL_CHANGE);

            // セッションに保存
            session.setAttribute("resetEmail_NewEmail", newEmail);

            // 再送ボタンから来た場合のみメッセージを表示
            if (isResend) {
                redirectAttributes.addFlashAttribute("message", "sent");
            }

            return "redirect:/email/verify-otp";
            
        } catch (RuntimeException e) {
            
            // クールタイムエラーの場合
            if (e.getMessage() != null && e.getMessage().contains("時間を空けて")) {
                
                // セッション復元
                session.setAttribute("resetEmail_NewEmail", newEmail);
                session.setAttribute("resetEmail_UserId", userId);

                redirectAttributes.addAttribute("error", "too_soon");
                return "redirect:/email/verify-otp";
            }

            redirectAttributes.addFlashAttribute("error", "エラーが発生しました");
            return "redirect:/email/reset-email";
        }
    }

    // OTP入力画面
    @GetMapping("/verify-otp")
    public String showVerifyOtpForm(HttpSession session) {
        if (session.getAttribute("resetEmail_NewEmail") == null) {
            return "redirect:/email/auth";
        }
        return "login/email_verify_otp"; 
    }

    // OTP検証と更新
    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam("otp") String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        String newEmail = (String) session.getAttribute("resetEmail_NewEmail");
        Integer userId = (Integer) session.getAttribute("resetEmail_UserId");

        if (newEmail == null || userId == null) return "redirect:/email/auth";

        boolean isValid;
        try {
            UsersEntity user = usersRepository.findById(userId).orElseThrow();
            // OtpServiceで検証
            isValid = otpService.verifyOtp(user, otp, OtpPurpose.EMAIL_CHANGE); 
            
            if (isValid) {
                // 検証成功 -> メールアドレス更新実行
                user.setEmail(newEmail);
                usersRepository.save(user);
                notificationMessageService.createEmailChangeCompletion(user);
                
                // トークン掃除
                otpService.clearOtp(user);
                
                // 完了通知
                notificationEmailService.sendEmailChangeCompletion(user);

                // セッション掃除
                session.removeAttribute("resetEmail_NewEmail");
                session.removeAttribute("resetEmail_UserId");

                return "login/email_complete";
            }
        } catch (Exception e) {
            isValid = false;
        }

        // 検証失敗時
        redirectAttributes.addAttribute("error", "invalid");
        return "redirect:/email/verify-otp";
    }
}