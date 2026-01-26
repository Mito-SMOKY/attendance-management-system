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

import com.example.attendancemanagementsystem.user.admin.dto.StatusRequestDto; // ★インポート追加
import com.example.attendancemanagementsystem.user.admin.service.StudentStatusRequestService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin/request/status")
public class StudentStatusRequestController {

    @Autowired
    private StudentStatusRequestService statusService;

    @GetMapping("/confirm")
    public String confirm(
            @RequestParam("ids") List<Integer> ids,
            Model model) {

        List<Map<String, Object>> studentList = statusService.getStudentDisplayData(ids);
        if (studentList.isEmpty()) {
            return "redirect:/admin/request/select?mode=status&error=noselection";
        }

        List<Map<String, Object>> approverList = statusService.getApproverList();
        List<Map<String, Object>> statusList = statusService.getStatusList();

        model.addAttribute("pageTitle", "ステータス変更申請");
        model.addAttribute("confirmMessage", "変更理由を入力してください（必須）");
        model.addAttribute("studentList", studentList);
        model.addAttribute("approverList", approverList);
        model.addAttribute("statusList", statusList);
        
        model.addAttribute("mode", "status"); 
        model.addAttribute("submitUrl", "/admin/request/status/submit");

        return "admin/request/requestStatusConfirm";
    }

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

            statusService.createStatusRequests(
                requestDto.getTargetIds(),
                requestDto.getTargetStatusId(),
                requestDto.getRemarks(),
                requestDto.getApproverId(),
                userDetails
            );
            return ResponseEntity.ok("ステータス変更申請が完了しました");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("エラーが発生しました: " + e.getMessage());
        }
    }
}