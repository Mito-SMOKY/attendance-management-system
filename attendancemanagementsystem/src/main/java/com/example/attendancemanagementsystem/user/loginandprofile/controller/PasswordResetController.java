package com.example.attendancemanagementsystem.user.loginandprofile.controller;

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
        String otp = passwordResetService.sendVerificationCode(email);

        if (otp != null) {
            session.setAttribute("resetEmail", email);
            session.setAttribute("resetOtp", otp);
            return "redirect:/password/verify"; 
        } else {
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
        
        if (correctOtp != null && correctOtp.equals(inputOtp)) {
            return "redirect:/password/new-password"; 
        } else {
            return "redirect:/password/verify?error"; 
        }
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
        
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "パスワードが一致しません");
            return "login/reset_password";
        }

        String email = (String) session.getAttribute("resetEmail");
        
        if (email != null) {
            passwordResetService.updatePassword(email, password);
            
            // セッション削除
            session.removeAttribute("resetEmail");
            session.removeAttribute("resetOtp");
            
            return "login/update"; 
        }
        
        return "redirect:/login";
    }
}