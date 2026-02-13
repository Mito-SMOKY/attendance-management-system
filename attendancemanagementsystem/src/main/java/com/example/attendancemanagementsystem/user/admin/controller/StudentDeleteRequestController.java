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

import com.example.attendancemanagementsystem.user.admin.dto.DeleteRequestDto;
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

        if (studentList.isEmpty()) {
            return "redirect:/admin/request/select?mode=delete&error=noselection";
        }
        
        model.addAttribute("pageTitle", "生徒削除確認");
        model.addAttribute("confirmMessage", "変更内容を入力してください（必須）");
        model.addAttribute("studentList", studentList);
        model.addAttribute("approverList", deleteService.getApproverList()); 
        
        // JS用パラメータ
        model.addAttribute("mode", "delete");
        model.addAttribute("submitUrl", "/admin/request/delete/submit");

        return "admin/request/requestDeleteConfirm";
    }

    // 申請処理実行 (一般管理者用 - POST)
    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<String> submit(
            @RequestBody DeleteRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            if (requestDto.getApproverId() == null) {
                return ResponseEntity.badRequest().body("承認者が選択されていません。");
            }

            deleteService.createDeleteRequests(requestDto, userDetails);
            return ResponseEntity.ok("削除申請が完了しました");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("エラーが発生しました: " + e.getMessage());
        }
    }

    // 即時実行 (上位管理者用 - POST)
    @PostMapping("/execute/delete")
    @ResponseBody
    public ResponseEntity<String> executeDelete(
            @RequestBody Map<String, Object> requestData,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            deleteService.executeDelete(requestData, userDetails);
            return ResponseEntity.ok("削除処理を完了しました。");
            
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("エラーが発生しました: " + e.getMessage());
        }
    }
}