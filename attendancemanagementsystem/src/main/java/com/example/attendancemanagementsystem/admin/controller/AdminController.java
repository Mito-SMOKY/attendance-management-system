package com.example.attendancemanagementsystem.admin.controller; // パッケージ名は適宜修正

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    /**
     * 管理者用メインメニュー（/admin/home）を表示
     */
    @GetMapping("/home")
    public String home() {
        return "admin/home"; //src/main/resources/templates/admin/home.html を参照
    }
}