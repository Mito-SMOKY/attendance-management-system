package com.example.attendancemanagementsystem.user.admin.controller; // パッケージ名は適宜修正

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
// @RequestMapping("/admin")
public class AdminController {

    /**
     * 管理者用メインメニュー（/admin/home）を表示
     */
    @GetMapping("/admin/timetable")
    public String home() {
        return "admin/timetable"; //src/main/resources/templates/admin/home.html を参照
    }
}