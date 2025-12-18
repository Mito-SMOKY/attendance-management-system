package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.enums.OtpPurpose;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.common.service.OtpService;
import com.example.attendancemanagementsystem.user.notification.service.NotificationEmailService;
import com.example.attendancemanagementsystem.user.notification.service.NotificationMessageService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/password")
public class PasswordResetController {

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

    // メールアドレス入力画面
    @GetMapping("/forgot")
    public String showForgotPasswordForm() {
        return "login/forgot_password";
    }

    // メール送信処理
    @PostMapping("/send-otp")
    public String processForgotPassword(@RequestParam("email") String email, HttpSession session, Model model) {
        try {
            // ユーザー検索
            UsersEntity user = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            // OTP送信 (パスワードリセット用)
            otpService.sendOtp(user, user.getEmail(), OtpPurpose.PASSWORD_RESET);
            
            // 成功した場合のみセッションに保存
            session.setAttribute("resetEmail", email);
            session.removeAttribute("isVerified");

            // 入力画面へ移動
            return "redirect:/password/verify"; 

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage()); 
            return "login/forgot_password";
        }
    }

    // 認証コード入力画面
    @GetMapping("/verify")
    public String showVerifyPage() {
        return "login/verify_otp";
    }

    // 認証コード検証処理
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") String inputOtp, HttpSession session, Model model) {
        String email = (String) session.getAttribute("resetEmail");

        if (email == null) {
            return "redirect:/password/forgot";
        }

        boolean isValid = false;
        try {
            // ユーザー検索
            UsersEntity user = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // OTP検証 (目的を指定)
            isValid = otpService.verifyOtp(user, inputOtp, OtpPurpose.PASSWORD_RESET);

        } catch (Exception e) {
            isValid = false;
        }

        if (!isValid) {
            return "redirect:/password/verify?error=invalid";
        }

        // 認証成功フラグをセッションに保存
        session.setAttribute("isVerified", true);

        return "redirect:/password/new-password";
    }
    
    // 新パスワード入力画面
    @GetMapping("/new-password")
    public String showNewPasswordForm(HttpSession session) {
        Boolean isVerified = (Boolean) session.getAttribute("isVerified");
        if (isVerified == null || !isVerified) {
            return "redirect:/password/verify";
        }
        return "login/reset_password"; 
    }

    // パスワード更新実行
    @PostMapping("/update")
    public String updatePassword(
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session, 
            Model model) {
        
        Boolean isVerified = (Boolean) session.getAttribute("isVerified");
        if (isVerified == null || !isVerified) {
            return "redirect:/login";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "パスワードが一致しません");
            return "login/reset_password";
        }

        String email = (String) session.getAttribute("resetEmail");
        
        if (email != null) {
            try {
                // ユーザー取得
                UsersEntity user = usersRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

                // パスワード更新
                user.setPassword(passwordEncoder.encode(password));
                usersRepository.save(user);

                // OTP後始末
                otpService.clearOtp(user);

                // 完了通知 (メール & サイト内)
                notificationEmailService.sendPasswordChangeCompletion(user);
                notificationMessageService.createPasswordChangeCompletion(user);
                
                // セッション破棄
                session.invalidate();
                return "login/update"; 

            } catch (RuntimeException e) {
                model.addAttribute("error", "更新に失敗しました");
                return "login/reset_password";
            }
        }
        
        return "redirect:/login";
    }
}