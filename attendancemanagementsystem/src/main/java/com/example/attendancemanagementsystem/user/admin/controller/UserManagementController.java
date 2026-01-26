package com.example.attendancemanagementsystem.user.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class UserManagementController {

    /**
     * ユーザー管理メニュー画面を表示
     * URL: /admin/userManagement
     */
    @GetMapping("/userManagement")
    public String showUserManagementMenu() {
        // htmlファイルの場所: src/main/resources/templates/admin/superAdmin/userManagement.html
        return "admin/superAdmin/userManagement";
    }
}