package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.user.loginandprofile.service.PasswordResetService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/password")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    // メールアドレス入力画面
    @GetMapping("/forgot")
    public String showForgotPasswordForm() {
        return "login/forgot_password";
    }

    // メール送信処理
    @PostMapping("/send-otp")
    public String processForgotPassword(@RequestParam("email") String email, HttpSession session, Model model) {
        try {
            String otp = passwordResetService.sendVerificationCode(email);
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(PasswordResetService.EXPIRY_MINUTES);

            session.setAttribute("resetEmail", email);
            session.setAttribute("resetOtp", otp);
            session.setAttribute("resetExpiry", expiryTime);

            return "redirect:/password/verify"; 

        } catch (RuntimeException e) {
            model.addAttribute("error", "メールアドレスが見つかりません。");
            return "login/forgot_password";
        }
    }

    // 認証コード入力画面
    @GetMapping("/verify")
    public String showVerifyPage() {
        return "login/verify_otp";
    }

    // 認証コードチェック
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") String inputOtp, HttpSession session) {
        String correctOtp = (String) session.getAttribute("resetOtp");
        LocalDateTime expiryTime = (LocalDateTime) session.getAttribute("resetExpiry");

        // 認証コードの検証
        if (correctOtp == null || !correctOtp.equals(inputOtp)) {
            return "redirect:/password/verify?error=invalid";
        }

        // 有効期限チェック
        if (expiryTime == null || LocalDateTime.now().isAfter(expiryTime)) {
            return "redirect:/password/verify?error=expired";
        }

        return "redirect:/password/new-password";
    }
    
    // 新パスワード入力画面
    @GetMapping("/new-password")
    public String showNewPasswordForm() {
        return "login/reset_password"; 
    }

    //パスワード更新処理
    @PostMapping("/update")
    public String updatePassword(
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session, 
            Model model) {
        
            // パスワードと確認用パスワードの一致チェック
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "パスワードが一致しません");
            return "login/reset_password";
        }

        String email = (String) session.getAttribute("resetEmail");
        
        // パスワード更新処理
        if (email != null) {
            passwordResetService.updatePassword(email, password);
            
            
            // セッション削除
            session.removeAttribute("resetEmail");
            session.removeAttribute("resetOtp");
            session.removeAttribute("resetExpiry");
            
            return "login/update"; 
        }
        
        return "redirect:/login";
    }
}