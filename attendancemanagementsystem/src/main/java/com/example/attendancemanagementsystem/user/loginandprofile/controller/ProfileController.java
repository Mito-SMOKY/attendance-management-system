package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.attendancemanagementsystem.user.loginandprofile.dto.ProfileSubjectDto;
import com.example.attendancemanagementsystem.user.loginandprofile.dto.UserProfileDto;
import com.example.attendancemanagementsystem.user.loginandprofile.service.ProfileService;

@Controller
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    //プロフィール画面を表示
    //URL: /profile
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        
        String loginId = userDetails.getUsername();

        // 1. ユーザー情報の取得
        UserProfileDto userData = profileService.getUserProfile(loginId);
        
        // 2. 教科リストの取得
        List<ProfileSubjectDto> subjectData = profileService.getUserSubjects(loginId);

        // 3. HTMLへのデータ渡し
        model.addAttribute("userData", userData);
        model.addAttribute("subjectData", subjectData);

        return "common/userDetail"; 
    }
}