package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.user.admin.service.RequestHistoryService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin/request/history")
public class RequestHistoryController {

    @Autowired
    private RequestHistoryService historyService;

    // 一覧画面
    @GetMapping
    public String list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        
        // 1. 生徒からの未承認申請 (Inbox)
        List<Map<String, Object>> studentRequests = historyService.getPendingStudentRequests();
        model.addAttribute("studentRequests", studentRequests);

        // 2. 自分の申請履歴 (Outbox)
        List<Map<String, Object>> historyList = historyService.getMyRequestHistory(userDetails.getUserId());
        model.addAttribute("historyList", historyList);
        
        model.addAttribute("pageTitle", "申請管理・履歴");
        
        return "admin/request/requestHistory";
    }

    // 詳細画面
    @GetMapping("/{id}")
    public String detail(
            @PathVariable("id") Integer requestId,
            Model model) {
        
        Map<String, Object> requestDetail = historyService.getRequestDetail(requestId);
        
        if (requestDetail == null) {
            return "redirect:/admin/request/history";
        }

        model.addAttribute("pageTitle", "申請詳細");
        model.addAttribute("detail", requestDetail);
        
        return "admin/request/requestDetail";
    }

    // 自分の申請を取り下げ
    @PostMapping("/{id}/withdraw")
    public String withdraw(
            @PathVariable("id") Integer requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            historyService.withdrawRequest(requestId, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "申請を取り下げました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "取り下げに失敗しました: " + e.getMessage());
        }
        return "redirect:/admin/request/history";
    }

    // 生徒の申請を承認
    @PostMapping("/{id}/approve")
    public String approve(
            @PathVariable("id") Integer requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            historyService.approveRequest(requestId, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "申請を承認しました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "承認に失敗しました: " + e.getMessage());
        }
        return "redirect:/admin/request/history";
    }

    // 生徒の申請を却下
    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable("id") Integer requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            historyService.rejectRequest(requestId, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "申請を却下しました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "却下に失敗しました: " + e.getMessage());
        }
        return "redirect:/admin/request/history";
    }
}