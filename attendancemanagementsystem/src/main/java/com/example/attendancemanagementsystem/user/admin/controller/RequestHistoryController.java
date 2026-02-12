package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping; // 追加
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // 追加

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
        
        // ログインユーザーのIDで履歴を取得
        List<Map<String, Object>> historyList = historyService.getMyRequestHistory(userDetails.getUserId());
        
        model.addAttribute("pageTitle", "申請履歴");
        model.addAttribute("historyList", historyList);
        
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

    // ★追加: 申請取り下げ処理
    @PostMapping("/{id}/withdraw")
    public String withdraw(
            @PathVariable("id") Integer requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            // サービス側の取り下げメソッドを呼び出し
            historyService.withdrawRequest(requestId, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "申請を取り下げました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "取り下げに失敗しました: " + e.getMessage());
        }
        
        return "redirect:/admin/request/history";
    }
}