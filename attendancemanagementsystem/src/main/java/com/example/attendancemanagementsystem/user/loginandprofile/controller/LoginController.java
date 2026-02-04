package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class LoginController {

    @Autowired
    private UsersRepository usersRepository;

    //共用メソッド
    private boolean isEmailNull(Principal principal) {
        if (principal == null) return false;
        
        //ユーザ情報の取得
        String loginId = principal.getName();
        UsersEntity user = usersRepository.findByLoginId(loginId).orElse(null);
        
        // ユーザーが存在し、かつEmailがNULLの場合に true を返す
        return user != null && user.getEmail() == null;
    }

    @GetMapping("/login")
    public String login(Principal principal, HttpServletRequest request) {
        
        // すでにログインしている場合
        if (principal != null) {
            
            // メールアドレス未登録なら強制的に変更画面へ
            if (isEmailNull(principal)) {
                return "redirect:/email/auth";
            }

            // 学生なら学生カレンダーへ
            if (request.isUserInRole("STUDENT")) {
                return "redirect:/student/main_calendar";
            }
            // 管理者なら管理者画面へ
            else if (request.isUserInRole("ADMIN") || request.isUserInRole("SUPER_ADMIN")) {
                return "redirect:/admin/timetable";
            }
            
            // どちらでもなければトップへ
            return "redirect:/login/login";
        }

        return "login/login";
    }
}