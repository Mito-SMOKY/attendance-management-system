package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import java.security.Principal;
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
    @GetMapping("/auth")
    public String showAuthForm(Principal principal, Model model, HttpSession session) {

        // ログイン中の場合、自動でユーザー情報を取得してリセット画面へ
        if (principal != null) {
            String loginId = principal.getName();
            UsersEntity user = usersRepository.findByLoginId(loginId).orElse(null);
            
            if (user != null) {

                // セッションにユーザーIDを保存してリセット画面へ
                session.setAttribute("resetEmail_UserId", user.getUserId());
                return "redirect:/email/reset-email";
            }
        }

        model.addAttribute("backUrl", "/login");
        return "login/email_auth";
    }

    // 本人確認処理
    @PostMapping("/auth")
    public String processAuth(
            @RequestParam("loginId") String loginId,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {
        
        UsersEntity user = usersRepository.findByLoginId(loginId).orElse(null);
        
        // 認証失敗
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "IDまたはパスワードが間違っています");
            return "login/email_auth";
        }

        session.setAttribute("resetEmail_UserId", user.getUserId());
        return "redirect:/email/reset-email";
    }

    // 新メールアドレス入力画面
    @GetMapping("/reset-email")
    public String showResetEmailForm(HttpSession session, Model model, Principal principal) {
        Integer userId = (Integer) session.getAttribute("resetEmail_UserId");
        if (userId == null) {
            return "redirect:/email/auth";
        }

        // 初回設定かどうか判定
        UsersEntity user = usersRepository.findById(userId).orElse(null);
        boolean isFirstSetup = (user != null && user.getEmail() == null);
        session.setAttribute("isFirstSetup", isFirstSetup);

        // バックボタンのURL設定
        if (principal != null) {
            model.addAttribute("backUrl", "/logout");
            model.addAttribute("isFirstLogin", isFirstSetup);
        } else {
            model.addAttribute("backUrl", "/email/auth");
            model.addAttribute("isFirstLogin", false);
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
        
        // OTP送信処理
        try {
            UsersEntity user = usersRepository.findById(userId).orElseThrow();

            // メールアドレスの重複チェック
            if (usersRepository.findByEmail(newEmail).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "このメールアドレスは既に使用されています");
                return "redirect:/email/reset-email";
            }

            // OTP送信
            otpService.sendOtp(user, newEmail, OtpPurpose.EMAIL_CHANGE);
            
            // 新しいメールアドレスをセッションに保存
            session.setAttribute("resetEmail_NewEmail", newEmail);
            return "redirect:/email/verify-otp";

        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg != null && msg.contains("時間を空けて")) {
                redirectAttributes.addFlashAttribute("error", "メール送信の制限がかかっています。少し時間を空けてから再度お試しください。");
            } else {
                redirectAttributes.addFlashAttribute("error", "予期せぬエラーが発生しました。");
            }
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
        
        // レスポンス用マップ
        Map<String, Object> response = new HashMap<>();
        String newEmail = (String) session.getAttribute("resetEmail_NewEmail");
        Integer userId = (Integer) session.getAttribute("resetEmail_UserId");
        Boolean isFirstSetup = (Boolean) session.getAttribute("isFirstSetup"); // ★追加
        
        // 必要な情報がセッションにない場合、認証画面へリダイレクト
        if (newEmail == null || userId == null) {
            response.put("success", true); 
            response.put("redirectUrl", "/email/auth");
            return response;
        }

        try {
            UsersEntity user = usersRepository.findById(userId).orElseThrow();
            
            boolean isValid = otpService.verifyOtp(user, otp, OtpPurpose.EMAIL_CHANGE); 
            
            // OTPが有効な場合、メールアドレスを更新
            if (isValid) {
                
                // メールアドレス更新
                if (usersRepository.findByEmail(newEmail).isPresent()) {
                    response.put("success", false);
                    response.put("message", "taken");
                    return response;
                }

                user.setEmail(newEmail);
                usersRepository.save(user);

                // 通知メール送信
                try {
                    notificationMessageService.createEmailChangeCompletion(user);
                    notificationEmailService.sendEmailChangeCompletion(user);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                
                otpService.clearOtp(user);
                session.removeAttribute("resetEmail_NewEmail");

                response.put("success", true);
                
                // 初回設定ならパスワード変更へリダイレクト
                if (isFirstSetup != null && isFirstSetup) {

                    // ユーザーID
                    response.put("redirectUrl", "/password/initial");
                } else {
                    session.removeAttribute("resetEmail_UserId");
                    session.removeAttribute("isFirstSetup");
                    response.put("redirectUrl", "/email/complete");
                }

            } else {
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