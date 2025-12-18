package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import org.springframework.beans.factory.annotation.Autowired;
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

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/password")
public class PasswordResetController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private UsersRepository usersRepository;

    // メールアドレス入力画面
    @GetMapping("/forgot")
    public String showForgotPasswordForm() {
        return "login/forgot_password";
    }

    // メール送信処理
    @PostMapping("/send-otp")
    public String processForgotPassword(
            @RequestParam("email") String email, 
            @RequestParam(name = "isResend", required = false, defaultValue = "false") boolean isResend,
            HttpSession session, 
            RedirectAttributes redirectAttributes) {
        try {
            UsersEntity user = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

            otpService.sendOtp(user, email, OtpPurpose.PASSWORD_RESET);
            
            session.setAttribute("resetEmail", email);
            session.removeAttribute("isVerified");

            if (isResend) {
                redirectAttributes.addFlashAttribute("message", "sent");
            }

            return "redirect:/password/verify"; 

        } catch (RuntimeException e) {
                // 再送信時の連打エラー処理
            if (e.getMessage() != null && e.getMessage().contains("時間を空けて")) {
                
                // セッションにメールアドレスを再設定
                session.setAttribute("resetEmail", email);
                
                redirectAttributes.addAttribute("error", "too_soon");
                return "redirect:/password/verify";
            }

            // 本当にユーザーが見つからない場合やその他のエラー
            redirectAttributes.addFlashAttribute("error", "メールアドレスが見つかりません。");
            return "redirect:/password/forgot";
        }
    }
    // 認証コード入力画面
    @GetMapping("/verify")
    public String showVerifyPage(HttpSession session) {
        if (session.getAttribute("resetEmail") == null) {
            return "redirect:/password/forgot";
        }
        return "login/verify_otp";
    }

   // 認証コード検証処理
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam("otp") String inputOtp, HttpSession session, RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("resetEmail");

        if (email == null) {
            return "redirect:/password/forgot";
        }

        boolean isValid;

        try {
            UsersEntity user = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            isValid = otpService.verifyOtp(user, inputOtp, OtpPurpose.PASSWORD_RESET);

        } catch (Exception e) {
            isValid = false;
        }

        if (!isValid) {
            redirectAttributes.addAttribute("error", "invalid");
            return "redirect:/password/verify";
        }

        // 認証成功
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
                // パスワード更新
                otpService.updatePassword(email, password);
                
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