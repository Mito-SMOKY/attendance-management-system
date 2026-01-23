package com.example.attendancemanagementsystem.user.admin.controller;

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
import com.example.attendancemanagementsystem.common.entity.DatalistDetailEntity; // ★追加
import com.example.attendancemanagementsystem.common.repository.DatalistDetailRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.user.admin.model.DatalistForm;
import com.example.attendancemanagementsystem.user.admin.model.ManualAccountForm;
import com.example.attendancemanagementsystem.user.admin.service.AdminCreateStudentService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin")
public class AdminCreateStudentController {

    @Autowired
    private AdminCreateStudentService adminService;

    @Autowired
    private DepartmentRepository departmentRepository; 

    @Autowired
    private DatalistDetailRepository datalistDetailRepository;

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUserId();
        }
        return 3;
    }

    //アカウント管理画面
    @GetMapping("/accountHome")
    public String showAccountHome() {
        return "admin/accountHome";
    }

    // ファイル読み込み画面表示用
    @GetMapping("/upload")
    public String showFileUploadPage() {
        return "admin/upload"; 
    }

    // ファイル受信時
    @PostMapping("/uploadFile") 
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            DatalistForm form = adminService.parseAccountFile(file);
            model.addAttribute("datalistForm", form);
            
            // プルダウン用に学科リストを渡す
            model.addAttribute("departmentList", departmentRepository.findAll());

            return "admin/uploadFile"; 

        } catch (Exception e) {
            model.addAttribute("errorMessage", "ファイルの読み込みに失敗しました: " + e.getMessage());
            return "admin/upload";
        }
    }

    //登録確認画面
    @PostMapping("/accountList")
    public String postAccountList(@ModelAttribute DatalistForm form, Model model) {

        // これで名前だけでなく、生徒リストや学科情報もすべて受け取れます
        model.addAttribute("datalistForm", form);
        return "admin/accountList"; 
    }

    //登録処理
    @PostMapping("/saveTempAccounts")
    public String saveTempAccounts(@ModelAttribute DatalistForm form, RedirectAttributes redirectAttributes) {
        
        // 重複があった場合は、そのIDリストが返ってくる
        List<String> duplicateIds = adminService.saveDatalist(form, getCurrentUserId());
        
        if (!duplicateIds.isEmpty()) {

            // 全件キャンセルされたことを伝える
            String message = "以下のIDで重複が検出されたため、登録処理を中止しました（データは保存されていません）: " 
                        + String.join(", ", duplicateIds);
            redirectAttributes.addFlashAttribute("warningMessage", message);

            return "redirect:/admin/upload";
            
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "すべてのデータが正常に登録されました。");

            return "redirect:/admin/tempAccountList/" + form.getDataListId() + "?origin=register";
        }
    }

    //仮アカウント一覧
    @GetMapping("/tempAccountList/{id}")
    public String tempAccountList(@PathVariable Integer id,
                                @RequestParam(required = false) String origin, 
                                Model model) {
        
        Datalist datalist = adminService.getDatalistById(id);
        model.addAttribute("datalist", datalist);
        List<DatalistDetailEntity> details = datalistDetailRepository.findByDatalistIdOrderByLoginIdAsc(id);
        model.addAttribute("details", details);

        // 戻るボタンのURLを動的に決める
        String backUrl; 

        if ("register".equals(origin)) {
            backUrl = "/admin/upload"; 

        } else {
            
            //履歴一覧から来た場合
            backUrl = "/admin/accountHistory";
        }

        model.addAttribute("backUrl", backUrl);
        
        return "admin/tempAccountList";
    }

    //手動入力画面
    @GetMapping("/manualInput")
    public String showManualAccountPage(Model model) {

        // プルダウン用に全学科・クラスを取得
        model.addAttribute("departmentList", departmentRepository.findAll());
        
        // フォームの初期化
        model.addAttribute("manualAccountForm", new ManualAccountForm());
        
        return "admin/manualInput";
    }

    //作成履歴画面
    @GetMapping("/accountHistory")
    public String showCreationHistory(Model model) {
        model.addAttribute("datalists", adminService.getAllDatalists());
        return "admin/accountHistory";
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

    // 手動入力データの保存 
    @PostMapping("/saveManualAccounts")
    public String saveManualAccounts(@ModelAttribute ManualAccountForm form, Model model) {
        adminService.saveDatalistFromForm(form, getCurrentUserId());
        
        // フォームデータを渡す
        model.addAttribute("manualForm", form);
        
        return "admin/manualResult";
    }

    // 手動登録完了後のCSVダウンロード 
    @PostMapping("/downloadManualCsv")
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
}
