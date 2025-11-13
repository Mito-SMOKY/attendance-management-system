package com.example.attendancemanagementsystem.user.loginandprofile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginForm() {
        return "login/login";
    }

    @GetMapping("/first-login")
    public String showFirstLoginForm() {
        return "login/first-login";
    }

    @GetMapping("/main-calendar")
    public String showMainCalendar() {
        return "student/main_calendar";
    }
}
