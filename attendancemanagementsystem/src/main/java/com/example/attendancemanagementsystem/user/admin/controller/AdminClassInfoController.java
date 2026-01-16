package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.attendancemanagementsystem.user.admin.dto.ClassInfoDto;
import com.example.attendancemanagementsystem.user.admin.service.AdminClassInfoService;

@Controller
@RequestMapping("/admin/class")
public class AdminClassInfoController {

    private final AdminClassInfoService adminClassInfoService;

    // 依存性注入
    public AdminClassInfoController(AdminClassInfoService adminClassInfoService) {
        this.adminClassInfoService = adminClassInfoService;
    }

    // 授業詳細画面を表示
    @GetMapping("/detail/{sessionId}")
    public String showClassDetail(
            @PathVariable("sessionId") Integer sessionId,
            @RequestParam(name = "from", required = false) String from,
            Model model) {
        
        // 詳細データを取得してModelに格納
        ClassInfoDto dto = adminClassInfoService.getClassInfo(sessionId);
        model.addAttribute("classInfo", dto);
        
        // 戻るボタンの遷移先URLを決定
        String backUrl;
        
        if ("timetable".equals(from)) {

            // 時間割画面から来た場合は時間割へ戻る
            backUrl = "/admin/timetable";
        } else {

            // それ以外は授業一覧画面へ戻る
            StringBuilder sb = new StringBuilder("/admin/dailyClassList");
            
            if (dto.getCurrentDateValue() != null) {
                sb.append("?date=").append(dto.getCurrentDateValue());
                
                // 教員のログインIDをパラメータに追加
                if (dto.getTeacherLoginId() != null) {
                    sb.append("&userId=").append(dto.getTeacherLoginId());
                }
            }
            backUrl = sb.toString();
        }
        
        // 戻り先URLを画面に渡す
        model.addAttribute("backUrl", backUrl);

        return "admin/classInfo";
    }

    // 授業情報の更新API
    @PostMapping("/session/update/{sessionId}")
    @ResponseBody
    public ResponseEntity<String> updateSessionInfo(
            @PathVariable("sessionId") Integer sessionId,
            @RequestBody Map<String, String> updateData) {
        try {

            // 授業情報の更新処理
            adminClassInfoService.updateSessionInfo(sessionId, updateData);
            return ResponseEntity.ok("授業情報を更新しました");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("更新に失敗しました");
        }
    }

    // 出欠情報の更新API
    @PostMapping("/attendance/update/{sessionId}")
    @ResponseBody
    public ResponseEntity<String> updateAttendance(
            @PathVariable("sessionId") Integer sessionId,
            @RequestBody Map<String, String> updateData) {
        try {

            // 出欠情報の更新処理
            adminClassInfoService.updateAttendance(sessionId, updateData);
            return ResponseEntity.ok("更新しました");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("更新に失敗しました");
        }
    }
}