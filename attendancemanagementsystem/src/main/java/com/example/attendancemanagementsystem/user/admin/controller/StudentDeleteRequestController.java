package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.user.admin.dto.DeleteRequestDto; // ★インポート追加
import com.example.attendancemanagementsystem.user.admin.service.StudentDeleteRequestService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin/request/delete")
public class StudentDeleteRequestController {

    @Autowired
    private StudentDeleteRequestService deleteService;

    // 確認画面表示 (GET)
    @GetMapping("/confirm")
    public String confirm(
            @RequestParam("ids") List<Integer> ids,
            Model model) {

        List<Map<String, Object>> studentList = deleteService.getStudentDisplayData(ids);

        // データがない場合は選択画面へ戻す
        if (studentList.isEmpty()) {
            return "redirect:/admin/request/select?mode=delete&error=noselection";
        }
        
        // 承認者リスト取得
        List<Map<String, Object>> approverList = deleteService.getApproverList(); 

        model.addAttribute("mode", "delete");
        model.addAttribute("pageTitle", "生徒削除確認");
        model.addAttribute("confirmMessage", "削除理由を入力してください（必須）");
        model.addAttribute("studentList", studentList);
        model.addAttribute("approverList", approverList);
        model.addAttribute("submitUrl", "/admin/request/delete/submit");

        return "admin/request/requestDeleteConfirm";
    }

    // 処理実行 (POST)
    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<String> submit(
            @RequestBody DeleteRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            // バリデーション
            if (requestDto.getApproverId() == null) {
                return ResponseEntity.badRequest().body("承認者が選択されていません。");
            }

            deleteService.createDeleteRequests(
                requestDto.getTargetIds(),
                requestDto.getRemarks(),
                requestDto.getApproverId(),
                userDetails
            );
            return ResponseEntity.ok("削除申請が完了しました");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("エラーが発生しました: " + e.getMessage());
        }
    }
}