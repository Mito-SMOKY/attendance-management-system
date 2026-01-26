package com.example.attendancemanagementsystem.user.superadmin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders; // PDF用
import org.springframework.http.MediaType;   // PDF用
import org.springframework.http.ResponseEntity; // PDF用
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;
import com.example.attendancemanagementsystem.user.superadmin.dto.SuperAdminCreateDto;
import com.example.attendancemanagementsystem.user.superadmin.dto.SuperAdminDetailDto;
import com.example.attendancemanagementsystem.user.superadmin.service.SuperAdminService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')") 
public class SuperAdminController {

    @Autowired
    private SuperAdminService superAdminService;

    // --- 1. ユーザー管理メニュー画面 ---
    @GetMapping("/sadminUserManagement")
    public String showUserManagement() {
        return "admin/sadminUserManagement";
    }

    // --- 2. 管理者一覧画面 ---
    @GetMapping("/sadminList")
    public String showAdminList(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "auth", required = false) String authFilter,
            Model model) {
        
        List<Map<String, Object>> sadminList = superAdminService.getAdminList(keyword, authFilter);
        model.addAttribute("sadminList", sadminList);
        model.addAttribute("keyword", keyword);
        model.addAttribute("authFilter", authFilter);
        
        return "admin/sadminList";
    }

    // --- 3. 詳細画面表示 ---
    @GetMapping("/sadminInfo/{id}")
    public String showAdminDetail(@PathVariable("id") Integer id, Model model) {
        SuperAdminDetailDto dto = superAdminService.getAdminDetail(id);
        model.addAttribute("sadminInfo", dto);
        return "admin/sadminInfo";
    }

    // --- 4. 更新処理 (有効化しました) ---
    @PostMapping("/sadmin/update")
    public String updateAdminDetail(
            @ModelAttribute SuperAdminDetailDto form,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            // 操作者IDを渡して更新実行
            superAdminService.updateAdmin(form, userDetails.getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "管理者情報を更新しました。");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "更新失敗: " + e.getMessage());
        }
        
        // 詳細画面へリダイレクト
        return "redirect:/admin/sadminInfo/" + form.getUserId();
    }

    // --- 5. 削除処理 (有効化しました) ---
    @PostMapping("/sadmin/delete")
    public String deleteAdmin(
            @RequestParam("targetID") Integer targetId,
            @RequestParam("password") String password,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        
        try {
            // 削除実行
            boolean isDeleted = superAdminService.deleteAdmin(targetId, password, userDetails.getUserId());
            
            if (isDeleted) {
                redirectAttributes.addFlashAttribute("successMessage", "管理者を削除しました。");
                return "redirect:/admin/sadminList"; // 一覧へ戻る
            } else {
                // パスワード間違いの場合
                redirectAttributes.addFlashAttribute("errorMessage", "パスワードが正しくありません。");
                return "redirect:/admin/sadminInfo/" + targetId;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "削除中にエラーが発生しました: " + e.getMessage());
            return "redirect:/admin/sadminInfo/" + targetId;
        }
    }
    
    // --- 6. 新規作成画面表示 ---
    @GetMapping("/sadminCreate")
    public String showAdminCreate(Model model) {
        if (!model.containsAttribute("adminForm")) {
            model.addAttribute("adminForm", new SuperAdminCreateDto());
        }
        return "admin/sadminCreate";
    }

    // --- 7. 新規作成実行 ---
    @PostMapping("/sadmin/create")
    public String createAdmin(
            @ModelAttribute SuperAdminCreateDto form,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Integer logId = superAdminService.createAdmin(form, userDetails.getUserId());
            return "redirect:/admin/sadminCreateSuccess?id=" + logId;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "登録失敗: " + e.getMessage());
            return "redirect:/admin/sadminCreate";
        }
    }

    // --- 8. 登録完了画面表示 ---
    @GetMapping("/sadminCreateSuccess")
    public String showCreateSuccess(@RequestParam("id") Integer logId, Model model) {
        model.addAttribute("logId", logId);
        return "admin/sadminCreateSuccess";
    }

    // --- 9. PDFダウンロード ---
    @GetMapping("/sadmin/download-pdf/{id}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable("id") Integer logId) {
        byte[] pdfBytes = superAdminService.generateRegistrationPdf(logId);
        String fileName = "admin_credentials_" + logId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(pdfBytes);
    }
}