package com.example.attendancemanagementsystem.user.superadmin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired; // 追加
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.user.superadmin.service.SuperAdminService; // 追加

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')") 
public class SuperAdminController {

    @Autowired // 追加
    private SuperAdminService superAdminService; // 追加

    // --- 1. ユーザー管理メニュー画面 ---
    @GetMapping("/sadminUserManagement")
    public String showUserManagement() {
        return "admin/sadminUserManagement";
    }

    // --- 2. 管理者一覧画面 ---
    @GetMapping("/sadminList")
    public String showAdminList(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "auth", required = false) String authFilter,
            Model model) {
        
        // ★修正: Serviceからデータを取得
        List<Map<String, Object>> sadminList = superAdminService.getAdminList(keyword, authFilter);

        model.addAttribute("sadminList", sadminList);
        // 検索条件を保持して画面に戻す（検索ボックスに値を残すため）
        model.addAttribute("keyword", keyword);

        model.addAttribute("authFilter", authFilter);
        
        return "admin/sadminList";
    }

    
}