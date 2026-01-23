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

    // @Autowired
    // private AdminSubjectService adminSubjectService;

    @Autowired
    private DepartmentRepository departmentRepository; // ★追加

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


    @GetMapping("/tmpAccount")
    public String showTmpAccount() {
        return "admin/tmpAccount";
    }

    @GetMapping("/accountManage")
    public String showAccountManage() {
        return "admin/accountManage";
    }

    @GetMapping("/upload")
    public String showFileUploadPage() {
        return "admin/csvUpload";
    }

    // ★修正箇所: アップロード後に csvName へ遷移
    // ★修正: handleFileUpload メソッド
    @PostMapping("/upload-file")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            DatalistForm form = adminService.parseAccountFile(file);
            model.addAttribute("datalistForm", form);
            
            // ★追加: csvName画面のプルダウン用に学科リストを渡す
            model.addAttribute("departmentList", departmentRepository.findAll());

            return "admin/csvName"; 
        } catch (Exception e) {
            model.addAttribute("errorMessage", "ファイルの読み込みに失敗しました: " + e.getMessage());
            return "admin/csvUpload";
        }
    }

    @PostMapping("/save-temp-accounts")
    public String saveTempAccounts(@ModelAttribute DatalistForm form, RedirectAttributes redirectAttributes) {
        
        // 重複があった場合は、そのIDリストが返ってくる（保存はされていない）
        List<String> duplicateIds = adminService.saveDatalist(form, getCurrentUserId());
        
        if (!duplicateIds.isEmpty()) {
            // ★メッセージ変更: 全件キャンセルされたことを伝える
            String message = "以下のIDで重複が検出されたため、登録処理を中止しました（データは保存されていません）: " 
                        + String.join(", ", duplicateIds);
            redirectAttributes.addFlashAttribute("warningMessage", message);
            
            // エラー時は登録確認画面に戻るなどの配慮も可能ですが、
            // 今回は仕様通り履歴画面(または一覧)へ戻します
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

    // @GetMapping("/manual-input")
    // public String showManualAccountPage() {
    //     return "admin/manual_input";
    // }

    @GetMapping("/manual-input")
    public String showManualAccountPage(Model model) {
        // ★追加: プルダウン用に全学科・クラスを取得して画面に渡す
        model.addAttribute("departmentList", departmentRepository.findAll());
        
        // フォームの初期化（空のリストを入れておくなど）
        model.addAttribute("manualAccountForm", new ManualAccountForm());
        
        return "admin/manual_input";
    }

    @GetMapping("/temp-account-list/{id}")
    public String showTempAccountList(@PathVariable("id") Integer id, Model model) {
        Datalist datalist = adminService.getDatalistById(id);
        model.addAttribute("datalist", datalist);
        
        // ★変更: メソッド名修正 (OrderByLoginIdAsc)
        List<DatalistDetailEntity> details = datalistDetailRepository.findByDatalistIdOrderByLoginIdAsc(id);
        model.addAttribute("details", details);
        
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
        
        // 2. 完了画面にフォームデータ（平文パスワード入り）を渡す
        model.addAttribute("manualForm", form);
        
        return "admin/manual_result";
    }

    // --- 11. 手動登録完了後のCSVダウンロード ---
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
    // ★修正: 引数を @ModelAttribute DatalistForm に変更
    @PostMapping("/account-list")
    public String postAccountList(@ModelAttribute DatalistForm form, Model model) {
        // これで名前だけでなく、生徒リスト(tempAccounts)や学科情報もすべて受け取れます
        model.addAttribute("datalistForm", form);
        return "admin/accountList"; 
    }
    
    // --- 11. マスタデータ管理メニュー画面 ---
    @GetMapping("/master-data")
    public String showMasterDataMenu() {
        return "admin/mdList"; 
    }

    // // --- 12. 教科情報マスタ詳細画面 (GET) ---
    // // リンクに合わせてURLを変更 (/mdSubjectInformation → /master/SubjectInformation)
    // @GetMapping("/master/SubjectInformation")
    // public String showSubjectInformation(Model model) {
    //     // テーブル表示用（ここはEntityのままでOKだが、念のため修正）
    //     model.addAttribute("subjectInfoList", adminSubjectService.getSubjectInfoList());
        
    //     // ドロップダウン用には「軽量版メソッド」を使う
    //     model.addAttribute("teacherList", adminSubjectService.getSimpleTeacherList());       // ← 変更
    //     model.addAttribute("majorList", adminSubjectService.getSimpleMajorList());           // ← 変更
    //     model.addAttribute("departmentList", adminSubjectService.getSimpleDepartmentList()); // ← 変更
    //     //これDB参照してない可用性0ゾーン
    //     model.addAttribute("gradeList", java.util.Arrays.asList(1, 2, 3)); 
        
    //     return "admin/mdSubjectInformation";
    // }

    // // --- 保存処理 (POST) ---
    // // こちらも合わせてURLを変更 (/mdSubjectInformation/save → /master/SubjectInformation/save)
    // @PostMapping("/master/SubjectInformation/save")
    // public String saveSubjectInformation(
    //         @RequestParam(name = "subjectId", required = false) List<Integer> subjectIds,
    //         @RequestParam(name = "subjectName", required = false) List<String> subjectNames,
    //         @RequestParam(name = "teacherId", required = false) List<Integer> teacherIds,
    //         @RequestParam(name = "courseCount", required = false) List<Integer> courseCounts,
    //         @RequestParam(name = "majorId", required = false) List<Integer> majorIds,
    //         @RequestParam(name = "departmentId", required = false) List<Integer> departmentIds,
    //         @RequestParam(name = "grade", required = false) List<Integer> grades,
    //         RedirectAttributes redirectAttributes) {
        
    //     try {
    //         // ▼ 修正: 引数を7つ渡すように変更
    //         adminSubjectService.saveSubjectList(
    //             subjectIds,
    //             subjectNames,
    //             teacherIds,
    //             courseCounts,
    //             majorIds,
    //             departmentIds,
    //             grades
    //         );
            
    //         redirectAttributes.addFlashAttribute("successMessage", "変更を保存しました。");
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         redirectAttributes.addFlashAttribute("errorMessage", "保存中にエラーが発生しました。");
    //     }

    //     //リダイレクト先も新しいURLに変更
    //     return "redirect:/admin/master/SubjectInformation";
    // }
}
