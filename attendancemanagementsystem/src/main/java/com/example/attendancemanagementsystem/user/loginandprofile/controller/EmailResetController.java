package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
import com.example.attendancemanagementsystem.user.notification.service.NotificationEmailService;
import com.example.attendancemanagementsystem.user.notification.service.NotificationMessageService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/email")
public class EmailResetController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;
    
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private NotificationEmailService notificationEmailService;
    
    @Autowired
    private NotificationMessageService notificationMessageService;

    // メールアドレス入力画面
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

    // OTP送信処理
    @PostMapping("/send-otp")
    public String sendOtp(
            @RequestParam("newEmail") String newEmail, 
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Integer userId = (Integer) session.getAttribute("resetEmail_UserId");
        if (userId == null) return "redirect:/email/auth";

        try {
            UsersEntity user = usersRepository.findById(userId).orElseThrow();
            
            // メール重複チェック
            if (usersRepository.findByEmail(newEmail).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "このメールアドレスは既に使用されています");
                return "redirect:/email/reset-email";
            }

            // OTP生成・送信
            otpService.sendOtp(user, newEmail, OtpPurpose.EMAIL_CHANGE);
            
            session.setAttribute("resetEmail_NewEmail", newEmail);
            return "redirect:/email/verify-otp";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "エラーが発生しました");
            return "redirect:/email/reset-email";
        }
    }

    // OTP入力画面
    @GetMapping("/verify-otp")
    public String showVerifyOtpForm(HttpSession session) {
        if (session.getAttribute("resetEmail_NewEmail") == null) {
            return "redirect:/email/reset-email";
        }
        return "login/email_verify_otp";
    }

    // 完了画面表示用メソッド
    @GetMapping("/complete")
    public String showCompletePage() {
        return "login/email_complete";
    }

    // OTP検証と更新
    @PostMapping("/verify-otp")
    @ResponseBody 
    public Map<String, Object> verifyOtp(
            @RequestParam("otp") String otp,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        String newEmail = (String) session.getAttribute("resetEmail_NewEmail");
        Integer userId = (Integer) session.getAttribute("resetEmail_UserId");

        // セッション切れの場合
        if (newEmail == null || userId == null) {
            response.put("success", true); 
            response.put("redirectUrl", "/email/auth");
            return response;
        }

        try {
            UsersEntity user = usersRepository.findById(userId).orElseThrow();
            
            // 検証実行
            boolean isValid = otpService.verifyOtp(user, otp, OtpPurpose.EMAIL_CHANGE); 
            
            if (isValid) {

                // 検証成功 -> 更新
                user.setEmail(newEmail);
                usersRepository.save(user);

                // 通知処理 
                try {
                    notificationMessageService.createEmailChangeCompletion(user);
                    notificationEmailService.sendEmailChangeCompletion(user);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                
                otpService.clearOtp(user);
                
                session.removeAttribute("resetEmail_NewEmail");
                session.removeAttribute("resetEmail_UserId");

                // 成功時のレスポンス 
                response.put("success", true);
                response.put("redirectUrl", "/email/complete"); 

            } else {
                // 失敗時のレスポンス 
                response.put("success", false);
                response.put("message", "invalid");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "error");
        }

        return response;
    }
}