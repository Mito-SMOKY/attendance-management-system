package com.example.attendancemanagementsystem.user.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.user.admin.dto.ClassListDto;
import com.example.attendancemanagementsystem.user.admin.service.AdminClassListService;

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
            @RequestParam(name = "userId", required = false, defaultValue = "admin001") String userId, 
            @RequestParam(name = "search", required = false) String searchWord,
            Model model) {

        // 日付が指定されていない場合は、システム日付（今日）を設定する
        if (date == null || date.isEmpty()) {
            date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        //DToから取得してセットする
        ClassListDto dto = adminClassListService.getDailyClassInfo(date, userId, searchWord);
        model.addAttribute("viewData", dto);
        model.addAttribute("currentUserId", userId);
        return "admin/classList";
    }
}