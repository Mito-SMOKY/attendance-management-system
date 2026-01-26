package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.attendancemanagementsystem.user.admin.service.RequestListService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin/approval")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class RequestListController {

    @Autowired
    private RequestListService approvalService;

    // 申請承認・履歴画面を表示
    @GetMapping("/list")
    public String list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        
        // ログイン中の上位管理者ID宛ての申請を取得
        List<Map<String, Object>> requestList = approvalService.getApprovalList(userDetails.getUserId());
        
        model.addAttribute("pageTitle", "申請承認・履歴");
        model.addAttribute("requestList", requestList);
        
        return "admin/superAdmin/RequestList";
    }
}