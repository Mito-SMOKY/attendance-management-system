package com.example.attendancemanagementsystem.user.admin.controller;

// 必要なクラスをインポート
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.common.entity.Datalist;
import com.example.attendancemanagementsystem.user.admin.model.DatalistForm;
import com.example.attendancemanagementsystem.user.admin.model.ManualAccountForm;
import com.example.attendancemanagementsystem.user.admin.service.AdminService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

// @Controller
// @RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUserId();
        }
        return 3; 
    }

    
    @GetMapping("/timetable")
    public String showTimetablePage() {
        return "admin/timetable";
    }

    @GetMapping("/home")
    public String home() {
        return "admin/home";
    }

    @GetMapping("/tmpAccount")
    public String showTmpAccountMenu() {
        return "admin/tmpAccount";
    }

    @GetMapping("/upload")
    public String showFileUploadPage() {
        return "admin/upload";
    }

    @PostMapping("/upload-file")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            DatalistForm form = adminService.parseAccountFile(file);
            model.addAttribute("datalistForm", form);
            return "admin/file_read_result";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "ファイルの読み込みに失敗しました: " + e.getMessage());
            return "admin/upload";
        }
    }

    @PostMapping("/save-temp-accounts")
    public String saveTempAccounts(@ModelAttribute DatalistForm form, RedirectAttributes redirectAttributes) {
        
        List<String> skippedIds = adminService.saveDatalist(form, getCurrentUserId());
        
        if (!skippedIds.isEmpty()) {
            String message = "以下のIDは重複しているため登録されませんでした: " + String.join(", ", skippedIds);
            redirectAttributes.addFlashAttribute("warningMessage", message);
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "すべてのデータが正常に登録されました。");
        }
        
        return "redirect:/admin/creation-history";
    }

    @GetMapping("/creation-history")
    public String showCreationHistory(Model model) {
        model.addAttribute("datalists", adminService.getAllDatalists());
        return "admin/creation_history";
    }

    @GetMapping("/manual-input")
    public String showManualAccountPage() {
        return "admin/manual_input";
    }

    // @PostMapping("/save-manual-accounts")
    // public String saveManualAccounts(@ModelAttribute ManualAccountForm form, RedirectAttributes redirectAttributes) {
    //     adminService.saveDatalistFromForm(form, getCurrentUserId());
        
    //     redirectAttributes.addFlashAttribute("successMessage", "手動登録が完了しました。");
    //     return "redirect:/admin/creation-history";
    // }

    @GetMapping("/temp-account-list/{id}")
    public String showTempAccountList(@PathVariable("id") Integer id, Model model) {
        // ★修正: common.entity.Datalist -> Datalist
        Datalist datalist = adminService.getDatalistById(id);
        model.addAttribute("datalist", datalist);
        return "admin/temp_account_list";
    }

    @GetMapping("/download-temp-file/{id}")
    public ResponseEntity<byte[]> downloadTempAccountFile(@PathVariable("id") Integer id) {
        
        byte[] csvData = adminService.createCsvFile(id);
        
        // ★修正: common.entity.Datalist -> Datalist
        Datalist datalist = adminService.getDatalistById(id);
        String fileName = datalist.getDataListName() + ".csv";
        
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (Exception e) {
            encodedFileName = "download.csv";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", encodedFileName);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }

    
    // ... (既存のコード) ...

    // --- 8. 手動入力データの保存 (★修正: 完了画面へ遷移) ---
    @PostMapping("/save-manual-accounts")
    public String saveManualAccounts(@ModelAttribute ManualAccountForm form, Model model) {
        // 1. DBへはハッシュ化して保存
        adminService.saveDatalistFromForm(form, getCurrentUserId());
        
        // 2. 完了画面にフォームデータ（平文パスワード入り）を渡す
        model.addAttribute("manualForm", form);
        
        return "admin/manual_result"; // 新しい画面へ
    }

    // --- 11. 手動登録完了後のCSVダウンロード (★新規追加) ---
    @PostMapping("/download-manual-csv")
    public ResponseEntity<byte[]> downloadManualCsv(@ModelAttribute ManualAccountForm form) {
        
        // 平文パスワード入りのCSVを生成
        byte[] csvData = adminService.createCsvFromForm(form);
        
        String fileName = form.getDatalistName() + ".csv";
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (Exception e) {
            encodedFileName = "download.csv";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", encodedFileName);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(csvData, headers, HttpStatus.OK);
    }
    
    // ... (既存のメソッド) ...

    // ... (前略) ...

    // --- 11. マスタデータ管理メニュー画面 ---
    @GetMapping("/master-data")
    public String showMasterDataMenu() {
        // ファイル名: masterDataMenu.html に合わせる
        return "admin/mdList"; 
    }

    // --- 12. 教科マスタ詳細画面 ---
    @GetMapping("/master/subject")
    public String showSubjectMaster() {
        // ファイル名: subjectMaster.html に合わせる
        return "admin/mdSubject";
    }

    // --- 13. 教室マスタ詳細画面 ---
    @GetMapping("/master/classroom")
    public String showClassroomMaster() {
        // ファイル名: classroomMaster.html に合わせる
        return "admin/mdClassroom";
    }

}
