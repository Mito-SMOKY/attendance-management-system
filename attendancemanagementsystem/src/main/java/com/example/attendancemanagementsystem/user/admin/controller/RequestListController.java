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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.user.admin.service.RequestListService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin/requestList")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class RequestListController {

    @Autowired
    private RequestListService requestListService;

    // 一覧画面 (GET /admin/requestList/list)
    @GetMapping("/list")
    public String list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        
        List<Map<String, Object>> requestList = requestListService.getApprovalList(userDetails.getUserId());
        model.addAttribute("pageTitle", "申請承認・履歴");
        model.addAttribute("requestList", requestList);
        
        return "admin/superAdmin/RequestList"; // ファイル名が RequestList.html ならこのままでOK
    }

    // 詳細画面 (GET /admin/requestList/detail/{id})
    @GetMapping("/detail/{id}")
    public String detail(
            @PathVariable("id") Integer requestId,
            Model model) {
        
        Map<String, Object> detail = requestListService.getRequestDetail(requestId);
        
        if (detail == null) {
            return "redirect:/admin/requestList/list";
        }

        model.addAttribute("pageTitle", "申請詳細");
        model.addAttribute("detail", detail);
        
        // ★修正箇所: ファイル名(requestDetail.html)に合わせて先頭を小文字にする
        return "admin/superAdmin/requestDetail";
    }

    // 承認・却下実行 (POST /admin/requestList/process)
    @PostMapping("/process")
    @ResponseBody
    public ResponseEntity<String> process(
            @RequestParam("requestId") Integer requestId,
            @RequestParam("action") String action) { 
        
        try {
            boolean isApproved = "approve".equals(action);
            requestListService.processRequest(requestId, isApproved);
            
            String msg = isApproved ? "承認しました。" : "却下しました。";
            return ResponseEntity.ok(msg);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("エラーが発生しました: " + e.getMessage());
        }
    }
}