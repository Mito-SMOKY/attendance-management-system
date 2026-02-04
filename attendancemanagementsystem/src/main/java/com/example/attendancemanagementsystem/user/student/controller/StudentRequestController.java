package com.example.attendancemanagementsystem.user.student.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;
import com.example.attendancemanagementsystem.user.student.dto.HistoryViewDto;
import com.example.attendancemanagementsystem.user.student.service.RequestService;

@Controller
@RequestMapping("/student")
public class StudentRequestController {

    private final RequestService requestService;

    public StudentRequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    // ▼▼▼ 復元: 申請メインメニュー画面 ▼▼▼
    @GetMapping("/request_main")
    public String showRequestMain() {
        return "student/request_main";
    }
    // ▲▲▲ 復元ここまで ▲▲▲

    // 申請フォーム画面の表示
    @GetMapping("/request/form")
    public String showRequestForm(Model model) {
        // 承認者（管理者）リストを取得して画面に渡す
        List<UsersEntity> admins = requestService.getAllAdmins();
        model.addAttribute("admins", admins);
        return "student/request_form";
    }

    // 申請フォームの送信処理
    @PostMapping("/request/form")
    public String submitRequest(
            @RequestParam("approverId") Integer approverId,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate,
            @RequestParam("periods") List<Integer> periods, 
            @RequestParam("reason") String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            // ServiceへUsersEntityを渡すよう修正
            requestService.createOfficialAbsenceRequest(
                approverId,
                userDetails.getUsersEntity(), // 修正ポイント
                startDate,
                endDate,
                periods,
                reason
            );

            redirectAttributes.addFlashAttribute("successMessage", "公欠申請を送信しました。");
            return "redirect:/student/request/history";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "申請に失敗しました: " + e.getMessage());
            return "redirect:/student/request/form";
        }
    }

    // 申請取り下げ処理
    @PostMapping("/request/cancel")
    public String cancelRequest(@RequestParam("requestId") Integer requestId,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            Integer userId = userDetails.getUsersEntity().getUserId();
            requestService.withdrawRequest(requestId, userId);
            
            redirectAttributes.addFlashAttribute("successMessage", "申請を取り下げました。");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "取り下げに失敗しました: " + e.getMessage());
        }
        return "redirect:/student/request/history";
    }

    // 申請履歴画面の表示
    @GetMapping("/request/history")
    public String showRequestHistory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Integer userId = userDetails.getUsersEntity().getUserId();
        
        List<RequestEntity> entities = requestService.getMyRequestHistory(userId);

        // DTO変換
        List<HistoryViewDto> historyList = entities.stream()
                .map(HistoryViewDto::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("historyList", historyList);
        return "student/request_history";
    }
}