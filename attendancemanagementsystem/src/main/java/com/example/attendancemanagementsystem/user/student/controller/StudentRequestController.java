package com.example.attendancemanagementsystem.user.student.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.user.student.service.RequestService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;
import com.example.attendancemanagementsystem.user.student.dto.HistoryViewDto;
import com.example.attendancemanagementsystem.user.student.form.StudentRequestForm;

@Controller
@RequestMapping("/student")
public class StudentRequestController {

    private final RequestService requestService;

    public StudentRequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping("/request_main")
    public String showRequestMenu() {
        return "student/request_main";
    }

    @GetMapping("/request/form")
    public String showRequestForm(Model model) {
        int currentYear = LocalDate.now().getYear();
        model.addAttribute("years", List.of(currentYear, currentYear + 1));
        
        List<UsersEntity> approvers = requestService.getAllAdmins();
        model.addAttribute("approvers", approvers);

        return "student/request_form";
    }

    @PostMapping("/request/form")
    public String submitRequest(StudentRequestForm form, 
                                @RequestParam(value = "approverId", required = false) Integer approverId, // 新規追加
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            // バリデーション
            if (approverId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "承認者を選択してください。");
                return "redirect:/student/request/form";
            }
            if (form.getPeriods() == null || form.getPeriods().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "時限を選択してください。");
                return "redirect:/student/request/form";
            }

            // サービス呼び出し (approverIdを追加)
            requestService.createOfficialAbsenceRequest(
                userDetails.getUserId(), 
                form.getStartDate(), 
                form.getEndDate(), 
                form.getPeriods(), 
                form.getReason(),
                approverId
            );

            redirectAttributes.addFlashAttribute("successMessage", "公欠申請を送信しました。");
            return "redirect:/student/request/history";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "申請に失敗しました: " + e.getMessage());
            return "redirect:/student/request/form";
        }
    }

    @PostMapping("/request/cancel")
    public String cancelRequest(@RequestParam("requestId") Integer requestId,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            requestService.withdrawRequest(requestId, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "申請を取り下げました。");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "取り下げに失敗しました: " + e.getMessage());
        }
        return "redirect:/student/request/history";
    }

    @GetMapping("/request/history")
    public String showRequestHistory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<RequestEntity> entities = requestService.getMyRequestHistory(userDetails.getUserId());

        List<HistoryViewDto> historyList = entities.stream()
                .map(HistoryViewDto::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("historyList", historyList);
        return "student/request_history";
    }
}