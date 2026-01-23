package com.example.attendancemanagementsystem.user.superadmin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired; // 追加
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.user.superadmin.dto.SuperAdminDetailDto;
import com.example.attendancemanagementsystem.user.superadmin.service.SuperAdminService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')") 
public class SuperAdminController {

    @Autowired // 追加
    private SuperAdminService superAdminService; // 追加

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
        
        // ★修正: Serviceからデータを取得
        List<Map<String, Object>> sadminList = superAdminService.getAdminList(keyword, authFilter);

        model.addAttribute("sadminList", sadminList);
        // 検索条件を保持して画面に戻す（検索ボックスに値を残すため）
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

    // --- 4. 更新処理 ---
    // @PostMapping("/sadmin/update")
    // public String updateAdminDetail(
    //         @ModelAttribute SuperAdminDetailDto form,
    //         RedirectAttributes redirectAttributes) {
        
    //     try {
    //         superAdminService.updateAdmin(form);
    //         redirectAttributes.addFlashAttribute("successMessage", "管理者情報を更新しました。");
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         redirectAttributes.addFlashAttribute("errorMessage", "更新に失敗しました。");
    //     }
        
    //      詳細画面へリダイレクト
    //     return "redirect:/admin/sadminInfo/" + form.getUserId();
    // }

    // --- 5. 削除処理 ---
    // @PostMapping("/sadmin/delete")
    // public String deleteAdmin(
    //         @RequestParam("targetID") Integer targetId,
    //         @RequestParam("password") String password,
    //         @AuthenticationPrincipal CustomUserDetails userDetails,
    //         RedirectAttributes redirectAttributes) {
        
    //     try {
    //         boolean isDeleted = superAdminService.deleteAdmin(targetId, password, userDetails.getUserId());
            
    //         if (isDeleted) {
    //             redirectAttributes.addFlashAttribute("successMessage", "管理者を削除しました。");
    //             return "redirect:/admin/sadminList"; // 一覧へ戻る
    //         } else {
    //             // パスワード間違いの場合
    //             redirectAttributes.addFlashAttribute("errorMessage", "パスワードが正しくありません。");
    //             return "redirect:/admin/sadminInfo/" + targetId;
    //         }
            
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         redirectAttributes.addFlashAttribute("errorMessage", "削除中にエラーが発生しました: " + e.getMessage());
    //         return "redirect:/admin/sadminInfo/" + targetId;
    //     }
    // }

    // --- 6. 新規作成画面表示 ---
    @GetMapping("/sadminCreate")
    public String showAdminCreate(Model model) {
        // 新規登録用なので、空のDTOを渡す（バリデーションエラー時の書き戻し用などに便利）
        // DTOがまだない場合は new SuperAdminDetailDto() だけでも動きますが、
        // 今後のためにフォームオブジェクトとして渡しておくと良いでしょう。
        if (!model.containsAttribute("adminForm")) {
            model.addAttribute("adminForm", new SuperAdminDetailDto());
        }
        return "admin/sadminCreate";
    }
}