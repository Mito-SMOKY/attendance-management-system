package com.example.attendancemanagementsystem.user.superadmin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.attendancemanagementsystem.user.superadmin.service.StudentOperationHistoryService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/history")
@RequiredArgsConstructor
public class StudentOperationHistoryController {

    private final StudentOperationHistoryService studentOperationHistoryService;

    /**
     * 生徒操作履歴画面を表示
     */
    @GetMapping("/studentOperation")
    public String showStudentOperationHistory(Model model) {
        // Serviceから履歴データを取得
        List<Map<String, Object>> historyList = studentOperationHistoryService.getStudentOperationHistory();
        
        // 画面に渡す
        model.addAttribute("historyList", historyList);
        
        // 表示するHTMLテンプレートを指定
        return "admin/superAdmin/studentOperationHistory";
    }
}