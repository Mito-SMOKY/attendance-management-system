package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.user.admin.service.RequestListService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin/requestList")
@PreAuthorize("hasRole('SUPER_ADMIN')") // スーパー管理者専用
public class RequestListController {

    @Autowired
    private RequestListService requestListService;

    // 一覧画面 (GET /admin/requestList/list)
    @GetMapping("/list")
    public String list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        
        // 未承認(1)などのリストを取得
        List<Map<String, Object>> requestList = requestListService.getApprovalList(userDetails.getUsersEntity().getUserId());
        model.addAttribute("requestList", requestList);
        model.addAttribute("pageTitle", "申請一覧");
        
        return "admin/superAdmin/requestList"; // スーパー管理者用の一覧へ
    }

    // 詳細画面 (GET /admin/requestList/detail?id=...)
    @GetMapping("/detail")
    public String detail(
            @RequestParam("id") Integer requestId,
            Model model) {
        
        // Serviceから詳細情報をMap形式で取得
        Map<String, Object> detail = requestListService.getRequestDetail(requestId);
        
        if (detail == null) {
            return "redirect:/admin/requestList/list";
        }

        model.addAttribute("pageTitle", "申請詳細");
        // HTML側では ${request} でデータにアクセスします
        model.addAttribute("request", detail);
        
        // ★修正箇所: 正しいスーパー管理者用テンプレートを指定
        return "admin/superAdmin/requestDetail"; 
    }

    // 承認・却下実行 (POST /admin/requestList/process)
    @PostMapping("/process")
    @ResponseBody // Ajaxで呼ばれることを想定
    public ResponseEntity<String> process(
            @RequestParam("requestId") Integer requestId,
            @RequestParam("action") String action,
            @AuthenticationPrincipal CustomUserDetails userDetails) { 
        
        try {
            boolean isApproved = "approve".equals(action);
            Integer approverId = userDetails.getUsersEntity().getUserId();
            
            // 承認処理を実行
            requestListService.processRequest(requestId, isApproved, approverId);
            
            String msg = isApproved ? "承認しました。" : "却下しました。";
            return ResponseEntity.ok(msg);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("処理に失敗しました: " + e.getMessage());
        }
    }
}