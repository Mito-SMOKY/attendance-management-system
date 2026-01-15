package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // 追加
import org.springframework.security.core.Authentication; // 追加
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder; // 追加
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // 追加
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.user.loginandprofile.dto.ProfileSubjectDto;
import com.example.attendancemanagementsystem.user.loginandprofile.dto.UserProfileDto;
import com.example.attendancemanagementsystem.user.loginandprofile.service.ProfileService;

@Controller
public class ProfileController {

    private final ProfileService profileService;
    private final UserDetailsService userDetailsService; // 追加: セッション更新用

    // コンストラクタでUserDetailsServiceも注入するよう変更
    public ProfileController(ProfileService profileService, UserDetailsService userDetailsService) {
        this.profileService = profileService;
        this.userDetailsService = userDetailsService;
    }

    // プロフィール画面を表示
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String loginId = userDetails.getUsername();
        
        UserProfileDto userData = profileService.getUserProfile(loginId);
        List<ProfileSubjectDto> subjectData = profileService.getUserSubjects(loginId);

        model.addAttribute("userData", userData);
        model.addAttribute("subjectData", subjectData);

        return "common/userDetail"; 
    }

    // プライバシー設定画面を表示
    @GetMapping("/privacySetting")
    public String showPrivacySetting(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String loginId = userDetails.getUsername();
        UserProfileDto userData = profileService.getUserProfile(loginId);
        
        model.addAttribute("userData", userData);
        
        return "common/privacySetting";
    }

    // 氏名更新API (Ajax用)
    @PostMapping("/api/profile/update-name")
    @ResponseBody
    public ResponseEntity<?> updateName(@AuthenticationPrincipal UserDetails userDetails, 
                                        @RequestBody Map<String, String> request) {
        
        String loginId = userDetails.getUsername();
        String newName = request.get("name");

        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "名前を入力してください"));
        }

        try {
            // 1. データベースを更新 (まずはDBを最新にする)
            profileService.updateUserName(loginId, newName);
            
            // 2. ▼▼▼ セッション情報の更新 (ここが重要！) ▼▼▼
            // DBが更新されたので、改めてユーザー情報をロードし直す
            UserDetails updatedUserDetails = userDetailsService.loadUserByUsername(loginId);
            
            // 現在の認証情報を取得
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            
            // 新しい情報で認証トークンを作り直す
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                updatedUserDetails, 
                currentAuth.getCredentials(), 
                updatedUserDetails.getAuthorities()
            );
            
            // セキュリティコンテキスト(記憶)に新しい情報をセットする
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

            return ResponseEntity.ok(Map.of("success", true, "message", "名前を更新しました"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "更新に失敗しました"));
        }
    }
}