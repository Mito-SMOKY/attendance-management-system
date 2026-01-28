package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import com.example.attendancemanagementsystem.user.admin.dto.StatusRequestDto;
import com.example.attendancemanagementsystem.user.admin.service.StudentStatusRequestService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin/request/status")
public class StudentStatusRequestController {

    @Autowired
    private StudentStatusRequestService statusService;

    // 確認画面表示 (GET)
    @GetMapping("/confirm")
    public String confirm(
            @RequestParam("ids") List<Integer> ids,
            Model model) {

        List<Map<String, Object>> studentList = statusService.getStudentDisplayData(ids);
        if (studentList.isEmpty()) {
            return "redirect:/admin/request/select?mode=status&error=noselection";
        }

        // 画面表示に必要なデータをModelにセット
        model.addAttribute("pageTitle", "ステータス変更申請");
        model.addAttribute("confirmMessage", "変更理由を入力してください（必須）");
        model.addAttribute("studentList", studentList);
        model.addAttribute("approverList", statusService.getApproverList());
        model.addAttribute("statusList", statusService.getStatusList());
        
        // JSで使用するパラメータ
        model.addAttribute("mode", "status"); 
        model.addAttribute("submitUrl", "/admin/request/status/submit");

        return "admin/request/requestStatusConfirm";
    }

    // 申請実行 (一般管理者用 - POST)
    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<String> submit(
            @RequestBody StatusRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            if (requestDto.getApproverId() == null) {
                return ResponseEntity.badRequest().body("承認者が選択されていません。");
            }
            if (requestDto.getTargetStatusId() == null) {
                return ResponseEntity.badRequest().body("変更後のステータスが選択されていません。");
            }

            // Serviceに処理を委譲
            statusService.createStatusRequests(requestDto, userDetails);
            
            return ResponseEntity.ok("ステータス変更申請が完了しました");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("エラーが発生しました: " + e.getMessage());
        }
    }

    // 即時実行 (上位管理者用 - POST)
    @PostMapping("/execute/status")
    @ResponseBody
    public ResponseEntity<String> executeStatus(
            @RequestBody Map<String, Object> requestData,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            // Serviceに処理（パスワード認証含む）を委譲
            statusService.executeStatusUpdate(requestData, userDetails);
            return ResponseEntity.ok("ステータス変更を完了しました。");

        } catch (SecurityException e) {
            // パスワード間違い等の認証エラー
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            // データ不足等の入力エラー
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // その他の予期せぬエラー
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("エラーが発生しました: " + e.getMessage());
        }
    }
}