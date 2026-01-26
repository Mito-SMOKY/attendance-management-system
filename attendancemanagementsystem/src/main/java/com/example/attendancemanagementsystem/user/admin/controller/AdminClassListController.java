package com.example.attendancemanagementsystem.user.admin.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.user.admin.dto.ClassListDto;
import com.example.attendancemanagementsystem.user.admin.service.AdminClassListService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails; 

@Controller
public class AdminClassListController {

    private final AdminClassListService adminClassListService;

    public AdminClassListController(AdminClassListService adminClassListService) {
        this.adminClassListService = adminClassListService;
    }

    // 授業一覧画面の表示
    @GetMapping("/admin/dailyClassList")
    public String showList(
            @RequestParam(name = "date", required = false) String date,
            @RequestParam(name = "userId", required = false) String userId, 
            @RequestParam(name = "search", required = false) String searchWord,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // ユーザーIDの決定
        if (userId == null || userId.isEmpty()) {
            if (userDetails != null) {
                userId = userDetails.getUsername();
            } else {
                userId = "admin001";
            }
        }

        if (date == null || date.isEmpty()) {
            
            // 指定がない場合は今日の日付 (yyyyMMdd)
            date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        } else {

            //データ整形
            date = date.replace("-", "").replace("/", "");
        }

        // DTOから取得してセット
        ClassListDto dto = adminClassListService.getDailyClassInfo(date, userId, searchWord);
        
        model.addAttribute("viewData", dto);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("selectedDate", date); 

        return "admin/classList";
    }
}