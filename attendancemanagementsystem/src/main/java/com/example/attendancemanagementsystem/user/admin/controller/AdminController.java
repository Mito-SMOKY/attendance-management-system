package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.user.admin.model.DatalistForm;
import com.example.attendancemanagementsystem.user.admin.model.ManualAccountForm;
import com.example.attendancemanagementsystem.user.admin.service.AdminService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ログイン中の管理者ID
    // ★先ほどDBに作った「999」を指定します
    private Integer getCurrentUserId() {
        return 3; 
    }

    // --- 1. メインメニュー ---
    @GetMapping("/home")
    public String home() {
        return "admin/home";
    }

    // --- 2. 仮アカウント管理メニュー ---
    @GetMapping("/tmpAccount")
    public String showTmpAccountMenu() {
        return "admin/tmpAccount";
    }

    // --- 3. ファイルアップロード画面表示 ---
    @GetMapping("/upload")
    public String showFileUploadPage() {
        return "admin/upload";
    }

    // --- 4. ファイル読み込み処理 (確認画面へ) ---
    @PostMapping("/upload-file")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            // ServiceでCSVを解析してフォームオブジェクトを受け取る
            DatalistForm form = adminService.parseAccountFile(file);
            model.addAttribute("datalistForm", form);
            return "admin/file_read_result";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "ファイルの読み込みに失敗しました: " + e.getMessage());
            return "admin/upload";
        }
    }

    // --- 5. データ保存処理 (修正版) ---
    @PostMapping("/save-temp-accounts")
    public String saveTempAccounts(@ModelAttribute DatalistForm form, RedirectAttributes redirectAttributes) {
        // Serviceから「重複してスキップされたIDリスト」を受け取る
        List<String> skippedIds = adminService.saveDatalist(form, getCurrentUserId());

        // 重複があれば警告メッセージを渡す、なければ成功メッセージ
        if (skippedIds != null && !skippedIds.isEmpty()) {
            String message = "以下のIDは重複しているため登録されませんでした: " + String.join(", ", skippedIds);
            redirectAttributes.addFlashAttribute("warningMessage", message);
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "すべてのデータが正常に登録されました。");
        }

        return "redirect:/admin/creation-history";
    }

    // --- 6. 作成履歴一覧表示 ---
    @GetMapping("/creation-history")
    public String showCreationHistory(Model model) {
        model.addAttribute("datalists", adminService.getDatalistsByCreator(getCurrentUserId()));
        return "admin/creation_history";
    }

    // --- 7. 手動入力画面表示 ---
    @GetMapping("/manual-input")
    public String showManualAccountPage() {
        return "admin/manual_input";
    }

    // --- 8. 手動入力データの保存処理 (★ここが追加箇所) ---
    @PostMapping("/save-manual-accounts")
    public String saveManualAccounts(@ModelAttribute ManualAccountForm form) {
        // Serviceを呼び出して保存
        adminService.saveDatalistFromForm(form, getCurrentUserId());
        
        // 保存後は履歴一覧へリダイレクト
        return "redirect:/admin/creation-history";
    }

    // --- 9. 詳細画面表示 (作成履歴から遷移) ---
    @GetMapping("/temp-account-list/{id}")
    public String showTempAccountList(@PathVariable("id") Integer id, Model model) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        // IDを使ってリスト情報を取得し、画面に渡す
        com.example.attendancemanagementsystem.common.entity.Datalist datalist = adminService.getDatalistById(id);
        model.addAttribute("datalist", datalist);

        return "admin/temp_account_list";
    }
}