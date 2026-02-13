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

import com.example.attendancemanagementsystem.user.admin.dto.DepartmentRequestDto;
import com.example.attendancemanagementsystem.user.admin.service.StudentDepartmentRequestService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
// ★修正1: URLを "department" に統一
@RequestMapping("/admin/request/department")
public class StudentDepartmentRequestController {

    @Autowired
    private StudentDepartmentRequestService departmentService;

    // 確認画面 (GET)
    @GetMapping("/confirm")
    public String confirm(
            @RequestParam("ids") List<Integer> ids,
            Model model) {

        List<Map<String, Object>> studentList = departmentService.getStudentDisplayData(ids);
        if (studentList.isEmpty()) {
            // ★修正2: リダイレクト先も department に合わせる（もし選択画面のURLが違うならそこに合わせてください）
            return "redirect:/admin/request/select?mode=course&error=noselection";
        }

        model.addAttribute("mode", "course");
        model.addAttribute("pageTitle", "学科・コース変更申請");
        model.addAttribute("confirmMessage", "変更内容を入力してください（任意）");
        
        model.addAttribute("studentList", studentList);
        model.addAttribute("departmentList", departmentService.getDepartmentList());
        model.addAttribute("approverList", departmentService.getApproverList());
        
        // ★修正3: 申請用URLを department に変更
        model.addAttribute("submitUrl", "/admin/request/department/submit");

        return "admin/request/requestDepartmentConfirm";
    }

    // 申請 (一般管理者用 - POST)
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
                return ResponseEntity.badRequest().body("移動先の学科・コースが選択されていません。");
            }

            departmentService.createDepartmentRequests(requestDto, userDetails);
            return ResponseEntity.ok("学科・コース変更申請が完了しました");
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("エラーが発生しました: " + e.getMessage());
        }
    }

    // 即時実行 (上位管理者用 - POST)
    // ★修正4: URLを execute/department に変更
    @PostMapping("/execute/department")
    @ResponseBody
    public ResponseEntity<String> executeDepartment(
            @RequestBody Map<String, Object> requestData,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        try {
            departmentService.executeDepartmentUpdate(requestData, userDetails);
            return ResponseEntity.ok("変更を完了しました。");
            
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