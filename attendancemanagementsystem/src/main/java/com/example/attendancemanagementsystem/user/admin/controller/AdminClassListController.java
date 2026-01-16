
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

    @GetMapping("/admin/dailyClassList")
    public String showList(
            @RequestParam(name = "date", required = false) String date,
            @RequestParam(name = "userId", required = false, defaultValue = "admin001") String userId,
            Model model) {

        // 日付がない場合は今日にする
        if (date == null || date.isEmpty()) {
            date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        ClassListDto dto = adminClassListService.getDailyClassInfo(date, userId);
        model.addAttribute("viewData", dto);

        return "admin/classList";
    }
}