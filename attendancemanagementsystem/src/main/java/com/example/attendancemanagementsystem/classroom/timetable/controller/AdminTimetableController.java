package com.example.attendancemanagementsystem.classroom.timetable.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.classroom.timetable.service.AdminTimetableService;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin")
public class AdminTimetableController {

    @Autowired
    private AdminTimetableService adminTimetableService;

    // ログイン中のユーザーIDを取得
    private String getCurrentUserLoginId(CustomUserDetails userDetails) {
        if (userDetails != null) {
            return userDetails.getUsername(); 
        }
        return "admin001"; 
    }

    // 画面表示用
    @GetMapping("/timetable")
    public String showTimetablePage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        // 全ての管理者を取得してプルダウン用に渡す
        List<UsersEntity> adminList = adminTimetableService.getAllAdmins();
        model.addAttribute("adminUsers", adminList);

        // 現在のログインユーザーのIDを渡す
        model.addAttribute("initialUserId", getCurrentUserLoginId(userDetails));

        return "admin/timetable";
    }

    // 時間割データの取得
    @GetMapping("/api/timetabledata")
    @ResponseBody
    public Map<String, Object> getTimetableData(@RequestParam("date") String dateStr,
                                                @RequestParam("userId") String targetLoginId) {
        return adminTimetableService.getTimetableData(dateStr, targetLoginId);
    }
}