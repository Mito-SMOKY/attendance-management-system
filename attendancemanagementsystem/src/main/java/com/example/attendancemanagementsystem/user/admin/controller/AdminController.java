package com.example.attendancemanagementsystem.user.admin.controller;

import java.io.IOException;
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

import com.example.attendancemanagementsystem.common.entity.Datalist;
import com.example.attendancemanagementsystem.user.admin.model.DatalistForm;
import com.example.attendancemanagementsystem.user.admin.model.ManualAccountForm;
import com.example.attendancemanagementsystem.user.admin.service.AdminService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/admin")
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

    // ★修正箇所: アップロード後に csvName へ遷移
    @PostMapping("/upload-file")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            DatalistForm form = adminService.parseAccountFile(file);
            model.addAttribute("datalistForm", form);
            return "admin/csvName"; 
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

    @GetMapping("/temp-account-list/{id}")
    public String showTempAccountList(@PathVariable("id") Integer id, Model model) {
        Datalist datalist = adminService.getDatalistById(id);
        model.addAttribute("datalist", datalist);
        return "admin/temp_account_list";
    }

    @GetMapping("/download-temp-file/{id}")
    public ResponseEntity<byte[]> downloadTempAccountFile(@PathVariable("id") Integer id) {
        
        byte[] csvData = adminService.createCsvFile(id);
        
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

    // --- 手動入力データの保存 ---
    @PostMapping("/save-manual-accounts")
    public String saveManualAccounts(@ModelAttribute ManualAccountForm form, Model model) {
        adminService.saveDatalistFromForm(form, getCurrentUserId());
        model.addAttribute("listName", form.getDataListName());
        return "admin/accountList"; 
    }

    // --- 手動登録完了後のCSVダウンロード ---
    @PostMapping("/download-manual-csv")
    public ResponseEntity<byte[]> downloadManualCsv(@ModelAttribute ManualAccountForm form) {
        
        byte[] csvData = adminService.createCsvFromForm(form);
        
        String fileName = form.getDataListName() + ".csv";
        
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

    // --- csvName.htmlからの遷移用 ---
    @PostMapping("/account-list")
    public String postAccountList(@RequestParam("dataListName") String dataListName, Model model) {
        model.addAttribute("listName", dataListName);
        return "admin/accountList"; 
    }

    // ★★★ 今回追加したメソッド（PDF/ZIP出力用） ★★★
    @PostMapping("/download-pdf")
    public void downloadPdf(
            @RequestParam(name = "useZip", required = false, defaultValue = "false") boolean useZip,
            @RequestParam(name = "zipFileName", required = false) String zipFileName,
            HttpServletResponse response) throws IOException {

        // --- PDF/ZIP作成ロジック（現時点ではダミーデータを返却） ---
        
        String fileName = "accounts_data";
        if (useZip) {
            if (zipFileName != null && !zipFileName.isEmpty()) {
                fileName = zipFileName;
            }
            if (!fileName.endsWith(".zip")) {
                fileName += ".zip";
            }
            response.setContentType("application/zip");
        } else {
            fileName += ".pdf";
            response.setContentType("application/pdf");
        }

        // ファイルダウンロードヘッダーの設定
        response.setHeader("Content-Disposition", "attachment; filename=" + 
                URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replace("+", "%20"));

        // ダミーデータ書き込み
        String dummyContent = "Test Data: PDF/ZIP generation logic needs to be implemented in AdminService.";
        response.getOutputStream().write(dummyContent.getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }

    // --- 既存のページ遷移用メソッド ---

    @GetMapping("/master-data")
    public String showMasterDataMenu() {
        return "admin/mdList"; 
    }

    @GetMapping("/master/subject")
    public String showSubjectMaster() {
        return "admin/mdSubject";
    }

    @GetMapping("/master/classroom")
    public String showClassroomMaster() {
        return "admin/mdClassroom";
    }

    @GetMapping("/accountManage")
    public String showaccountManage() {
        return "admin/accountManage";
    }  
    @GetMapping("/timeTableEdit")
    public String showtimeTableEdit() {
        return "admin/timeTableEdit";
    }  
    @GetMapping("/sessionMenu")
    public String showsessionMenu() {
        return "admin/sessionMenu";
    }  
    @GetMapping("/mdSubject")
    public String showmdSubject() {
        return "admin/mdSubject";
    }  
    @GetMapping("/accountHistory")
    public String showaccountHistory() {
        return "admin/accountHistory";
    }  
    @GetMapping("/csvUpload")
    public String showcsvUpload() {
        return "admin/csvUpload";
    }  
    @GetMapping("/csvName")
    public String showcsvName() {
        return "admin/csvName";
    }  
    @GetMapping("/accountInput")
    public String showaccountInput() {
        return "admin/accountInput";
    }  
    
    @GetMapping("/accountList")
    public String showaccountList() {
        return "admin/accountList";
    }

    @GetMapping("/accountOutput")
    public String showaccountOutput() {
        return "admin/accountOutput";
    }  
}