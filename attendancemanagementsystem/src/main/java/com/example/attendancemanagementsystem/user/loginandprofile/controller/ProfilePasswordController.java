package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/profile/security/password")
public class ProfilePasswordController {

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private OtpService otpService;

    // OTPリクエスト画面
    @GetMapping("/request-otp")
    public String requestOtpForPasswordChange(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        UsersEntity user = usersRepository.findById(userDetails.getUserId()).orElseThrow();

        try {
            otpService.sendOtp(user, user.getEmail(), OtpPurpose.PASSWORD_CHANGE);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "メールの再送信間隔が短すぎます。既存のコードを使用してください。");
        }
        return "redirect:/profile/security/password/verify";
    }
    // 再送信処理
    @PostMapping("/resend-otp")
    public String resendOtpForPasswordChange(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            UsersEntity user = usersRepository.findById(userDetails.getUserId()).orElseThrow();
            otpService.sendOtp(user, user.getEmail(), OtpPurpose.PASSWORD_CHANGE);
            redirectAttributes.addFlashAttribute("message", "sent");
            return "redirect:/profile/security/password/verify";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addAttribute("error", "too_soon");
            return "redirect:/profile/security/password/verify";
        }
    }

    // OTP入力画面
    @GetMapping("/verify")
    public String showPasswordOtpPage() {
        return "common/password_verify_otp";
    }

    // OTP検証
    @PostMapping("/verify")
    public String verifyPasswordOtp(
            @RequestParam("otp") String otp,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        UsersEntity user = usersRepository.findById(userDetails.getUserId()).orElseThrow();
        
        if (otpService.verifyOtp(user, otp, OtpPurpose.PASSWORD_CHANGE)) {
            session.setAttribute("passwordChangeVerified", true);
            return "redirect:/profile/security/password/new";
        } else {
            redirectAttributes.addAttribute("error", "invalid");
            return "redirect:/profile/security/password/verify";
        }
    }

    // 新パスワード入力画面
    @GetMapping("/new")
    public String showNewPasswordPage(HttpSession session) {
        if (session.getAttribute("passwordChangeVerified") == null) {
            return "redirect:/profile/security/password/request-otp";
        }
        return "common/reset_password";
    }

    // パスワード更新実行
    @PostMapping("/update")
    public String updatePassword(
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("passwordChangeVerified") == null) {
            return "redirect:/login";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "パスワードが一致しません");
            return "redirect:/profile/security/password/new";
        }

        UsersEntity user = usersRepository.findById(userDetails.getUserId()).orElseThrow();
        
        // パスワード更新
        otpService.updatePassword(user.getEmail(), password); 

        // セッションフラグの消去
        session.removeAttribute("passwordChangeVerified");

        return "redirect:/profile/security/password/complete";
    }

    // 完了画面
    @GetMapping("/complete")
    public String showPasswordCompletePage() {
        return "common/update";
    }
}