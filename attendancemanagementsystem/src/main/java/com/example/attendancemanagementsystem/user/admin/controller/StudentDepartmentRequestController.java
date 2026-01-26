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

import com.example.attendancemanagementsystem.user.admin.dto.DepartmentRequestDto; // ★インポート追加
import com.example.attendancemanagementsystem.user.admin.service.StudentDepartmentRequestService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin/request/department")
public class StudentDepartmentRequestController {

    @Autowired
    private StudentDepartmentRequestService departmentService;

    /**
     * 確認画面表示 (GET)
     */
    @GetMapping("/confirm")
    public String confirm(
            @RequestParam("ids") List<Integer> ids,
            Model model) {

        List<Map<String, Object>> studentList = departmentService.getStudentDisplayData(ids);

        if (studentList.isEmpty()) {
            return "redirect:/admin/request/select?mode=course&error=noselection";
        }

        List<Map<String, Object>> approverList = departmentService.getApproverList();
        List<Map<String, Object>> departmentList = departmentService.getDepartmentList();

        model.addAttribute("pageTitle", "学科・コース変更申請");
        model.addAttribute("confirmMessage", "変更理由を入力してください（必須）");
        
        model.addAttribute("studentList", studentList);
        model.addAttribute("approverList", approverList);
        model.addAttribute("departmentList", departmentList);
        
        model.addAttribute("mode", "course"); 
        model.addAttribute("submitUrl", "/admin/request/department/submit");

        return "admin/request/requestDepartmentConfirm";
    }

    /**
     * 申請実行 (POST)
     */
    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<String> submit(
            @RequestBody DepartmentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            if (requestDto.getApproverId() == null) {
                return ResponseEntity.badRequest().body("承認者が選択されていません。");
            }
            if (requestDto.getTargetDepartmentId() == null) {
                return ResponseEntity.badRequest().body("変更先の学科・コースが選択されていません。");
            }

            departmentService.createDepartmentRequests(
                requestDto.getTargetIds(),
                requestDto.getTargetDepartmentId(),
                requestDto.getRemarks(),
                requestDto.getApproverId(),
                userDetails
            );
            return ResponseEntity.ok("学科・コース変更申請が完了しました");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("エラーが発生しました: " + e.getMessage());
        }
    }
}