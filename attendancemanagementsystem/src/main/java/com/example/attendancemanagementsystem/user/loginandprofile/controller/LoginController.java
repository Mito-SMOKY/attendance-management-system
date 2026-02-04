package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Principal principal, HttpServletRequest request) {
        
        // すでにログインしている場合
        if (principal != null) {
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

    @GetMapping("/first-login")
    public String showFirstLoginForm() {
        return "login/first-login";
    }
}

