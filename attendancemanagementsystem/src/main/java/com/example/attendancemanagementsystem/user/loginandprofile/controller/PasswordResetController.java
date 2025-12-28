package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody; // 追加
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
            
            // ★以前のアドバイス通り、可能ならここで userId も保存した方が安全です
            session.setAttribute("resetEmail", email);
            session.removeAttribute("isVerified");

            if (isResend) {
                redirectAttributes.addFlashAttribute("message", "sent");
            }

            return "redirect:/password/verify-otp"; 

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("時間を空けて")) {
                session.setAttribute("resetEmail", email);
                redirectAttributes.addAttribute("error", "too_soon");
                return "redirect:/password/verify-otp";
            }
            redirectAttributes.addFlashAttribute("error", "メールアドレスが見つかりません。");
            return "redirect:/password/forgot";
        }
    }

    // 認証コード入力画面 (GET)
    @GetMapping("/verify-otp")
    public String showVerifyOtpForm(HttpSession session) {
        if (session.getAttribute("resetEmail") == null) {
            return "redirect:/password/forgot";
        }
        return "login/verify_otp";
    }

    //otp検証
    @PostMapping("/verify-otp")
    @ResponseBody 
    public Map<String, Object> verifyOtp(
            @RequestParam("otp") String inputOtp, 
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        String email = (String) session.getAttribute("resetEmail");

        if (email == null) {
            response.put("success", false);
            response.put("message", "セッションが切れました。最初からやり直してください。");
            return response;
        }

        boolean isValid = false;
        try {
            UsersEntity user = usersRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 検証実行
            isValid = otpService.verifyOtp(user, inputOtp.trim(), OtpPurpose.PASSWORD_RESET);

        } catch (Exception e) {
            e.printStackTrace();
            isValid = false;
        }

        if (isValid) {
            // 成功時
            session.setAttribute("isVerified", true);
            response.put("success", true);
            response.put("redirectUrl", "/password/new-password"); 
        } else {
            // 失敗時
            response.put("success", false);
            response.put("message", "invalid");
        }

        return response;
    }

    // 新パスワード入力画面 (GET)
    @GetMapping("/new-password")
    public String showNewPasswordForm(HttpSession session) {
        Boolean isVerified = (Boolean) session.getAttribute("isVerified");
        if (isVerified == null || !isVerified) {
            return "redirect:/password/verify-otp";
        }
        return "login/reset_password"; 
    }

    // パスワード更新実行 (POST)
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
                otpService.updatePassword(email, password);
                
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