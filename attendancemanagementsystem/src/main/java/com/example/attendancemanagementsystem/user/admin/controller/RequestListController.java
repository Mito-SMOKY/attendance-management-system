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
    public String list(Model model) {
        
        // 1. Serviceからリストを取得
        List<Map<String, Object>> requests = requestListService.getRequestList();
        
        // 2. 画面に渡す
        model.addAttribute("requestList", requests);
        
        model.addAttribute("pageTitle", "申請承認・履歴一覧");
        
        // 3. テンプレートを返す
        // 【修正】実際のファイル配置に合わせてパスを変更
        return "admin/superAdmin/requestList"; 
    }

    // 詳細画面 (GET /admin/requestList/detail)
    @GetMapping("/detail")
    public String detail(
            @RequestParam("id") Integer requestId,
            Model model) {
        
        Map<String, Object> detail = requestListService.getRequestDetail(requestId);
        
        if (detail == null) {
            return "redirect:/admin/requestList/list";
        }

        model.addAttribute("pageTitle", "申請詳細");
        model.addAttribute("request", detail);
        
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
            return ResponseEntity.badRequest().body("処理に失敗しました。");
        }
    }
}