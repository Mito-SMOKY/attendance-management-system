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

import com.example.attendancemanagementsystem.common.entity.ClassroomEntity;
import com.example.attendancemanagementsystem.common.entity.Datalist; // ★追加
import com.example.attendancemanagementsystem.user.admin.model.DatalistForm;
import com.example.attendancemanagementsystem.user.admin.model.ManualAccountForm;
import com.example.attendancemanagementsystem.user.admin.service.AdminClassroomService;
import com.example.attendancemanagementsystem.user.admin.service.AdminCourseService;
import com.example.attendancemanagementsystem.user.admin.service.AdminDepartmentService;
import com.example.attendancemanagementsystem.user.admin.service.AdminMajorService;
import com.example.attendancemanagementsystem.user.admin.service.AdminService;
import com.example.attendancemanagementsystem.user.admin.service.AdminSubjectService; // ★追加
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminSubjectService adminSubjectService;

    @Autowired
    private AdminClassroomService adminClassroomService; // ★追加

    @Autowired
    private AdminDepartmentService adminDepartmentService; // ★追加

    @Autowired
    private AdminMajorService adminMajorService;       // 追加

    @Autowired
    private AdminCourseService adminCourseService;     // 追加


    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUserId();
        }
        return 3;
    }

    @GetMapping("/subjectList")
    public String showSubjectList() {
        return "admin/subjectList";
    }

    @GetMapping("/subjectAttendance")
    public String showSubjectAttendance() {
        return "admin/subjectAttendance";
    }

    @GetMapping("/studentList")
    public String showStudentList() {
        return "admin/studentList";
    }

    @GetMapping("/studentInfo")
    public String showStudentInfo() {
        return "admin/studentInfo";
    }

    @GetMapping("/timeTableList")
    public String showtimeTableList() {
        return "admin/timeTableList";
    }

    @GetMapping("/timeTableEdit")
    public String showtimeTableEdit() {
        return "admin/timeTableEdit";
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
    @PostMapping("/account-list")
    public String postAccountList(@RequestParam("dataListName") String dataListName, Model model) {
        model.addAttribute("listName", dataListName);
        return "admin/accountList"; 
    }
    
    // --- 11. マスタデータ管理メニュー画面 ---
    @GetMapping("/master-data")
    public String showMasterDataMenu() {
        return "admin/mdList"; 
    }

    //  --- 12. 教科マスタ詳細画面 ---
    // @GetMapping("/master/subject")
    // public String showSubjectMaster(Model model) {
    //     // クラス(学科)リストを取得 (IDを使うためEntityのリストを渡す)
    //     model.addAttribute("departmentList", adminSubjectService.getAllDepartments());
        
    //     // マトリクスデータ(行データ)を取得
    //     model.addAttribute("subjectDetailList", adminSubjectService.getSubjectMatrixData());
        
    //     return "admin/mdSubject";
    // }

    // --- 教科マスタ保存 (POST) ---
    // @PostMapping("/master/subject/save")
    // public String saveSubjectMaster(
    //         // チェックされたセルの値 ("subjectId-departmentId") をリストで受け取る
    //         @RequestParam(name = "activePairs", required = false) List<String> activePairs,
    //         RedirectAttributes redirectAttributes) {
        
    //     try {
    //         adminSubjectService.saveSubjectMatrix(activePairs);
    //         redirectAttributes.addFlashAttribute("successMessage", "教科とクラスの紐づけを保存しました。");
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
    //     }

    //     return "redirect:/admin/master/subject";
    // }

    // --- 13. 教室マスタ詳細画面 (一覧表示) ---
    // Serviceからデータを取得して画面に渡すように変更
    @GetMapping("/master/classroom")
    public String showClassroomMaster(Model model) {
        List<ClassroomEntity> list = adminClassroomService.getAllClassrooms();
        model.addAttribute("classroomList", list);
        return "admin/mdClassroom";
    }

    // --- 13.5 教室マスタ保存 (POST) ---
    // ★追加: 編集・削除・追加を一括保存する処理
    @PostMapping("/master/classroom/save")
    public String saveClassroomMaster(
            @RequestParam(name = "classroomId", required = false) List<Integer> classroomIds,
            @RequestParam(name = "classroomName", required = false) List<String> classroomNames,
            @RequestParam(name = "macAddress", required = false) List<String> macAddresses,
            RedirectAttributes redirectAttributes) {
        
        try {
            adminClassroomService.saveClassroomList(classroomIds, classroomNames, macAddresses);
            redirectAttributes.addFlashAttribute("successMessage", "教室情報を保存しました。");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
        }

        return "redirect:/admin/master/classroom";
    }

    // --- 12. 教科情報マスタ詳細画面 (GET) ---
    // リンクに合わせてURLを変更 (/mdSubjectInformation → /master/SubjectInformation)
    @GetMapping("/master/SubjectInformation")
    public String showSubjectInformation(Model model) {
        // テーブル表示用（ここはEntityのままでOKだが、念のため修正）
        model.addAttribute("subjectInfoList", adminSubjectService.getSubjectInfoList());
        
        // ドロップダウン用には「軽量版メソッド」を使う
        model.addAttribute("teacherList", adminSubjectService.getSimpleTeacherList());       // ← 変更
        model.addAttribute("courseList", adminSubjectService.getSimpleCourseList());           // ← 変更
        model.addAttribute("departmentList", adminSubjectService.getSimpleDepartmentList()); // ← 変更
        //これDB参照してない可用性0ゾーン
        model.addAttribute("gradeList", java.util.Arrays.asList(1, 2, 3)); 
        
        return "admin/mdSubjectInformation";
    }

    // --- 保存処理 (POST) ---
    // ★修正: こちらも合わせてURLを変更 (/mdSubjectInformation/save → /master/SubjectInformation/save)
    @PostMapping("/master/SubjectInformation/save")
    public String saveSubjectInformation(
            @RequestParam(name = "subjectId", required = false) List<Integer> subjectIds,
            @RequestParam(name = "subjectName", required = false) List<String> subjectNames,
            @RequestParam(name = "teacherId", required = false) List<Integer> teacherIds,
            @RequestParam(name = "courseCount", required = false) List<Integer> courseCounts,
            @RequestParam(name = "courseId", required = false) List<Integer> courseIds,
            @RequestParam(name = "departmentId", required = false) List<Integer> departmentIds,
            @RequestParam(name = "grade", required = false) List<Integer> grades,
            RedirectAttributes redirectAttributes) {
        
        try {
            // ▼ 修正: 引数を7つ渡すように変更
            adminSubjectService.saveSubjectList(
                subjectIds,
                subjectNames,
                teacherIds,
                courseCounts,
                courseIds,
                departmentIds,
                grades
            );
            
            redirectAttributes.addFlashAttribute("successMessage", "変更を保存しました。");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "保存中にエラーが発生しました。");
        }

        //リダイレクト先も新しいURLに変更
        return "redirect:/admin/master/SubjectInformation";
    }

    // // --- 14. 所属(クラス)マスタ詳細画面 (GET) ---
    // @GetMapping("/master/department")
    // public String showDepartmentMaster(Model model) {
    //     // 一覧表示用データ
    //     List<DepartmentEntity> deptList = adminDepartmentService.getAllDepartments();
    //     model.addAttribute("departmentList", deptList);
        
    //     // ドロップダウン用コースリスト
    //     model.addAttribute("courseList", adminDepartmentService.getAllCourses());
        
    //     return "admin/mdDepartment"; // 新規作成するHTML
    // }

    // // --- 所属(クラス)マスタ保存 (POST) ---
    // @PostMapping("/master/department/save")
    // public String saveDepartmentMaster(
    //         @RequestParam(name = "departmentId", required = false) List<Integer> departmentIds,
    //         @RequestParam(name = "className", required = false) List<String> classNames,
    //         @RequestParam(name = "courseId", required = false) List<Integer> courseIds,
    //         RedirectAttributes redirectAttributes) {
        
    //     try {
    //         adminDepartmentService.saveDepartmentList(departmentIds, classNames, courseIds);
    //         redirectAttributes.addFlashAttribute("successMessage", "所属情報を保存しました。");
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
    //     }

    //     return "redirect:/admin/master/department";
    // }


    // ==========================================
    // 所属マスタ機能 (3タブ構成)
    // ==========================================

    // --- Tab 1: 学科マスタ ---
    @GetMapping("/master/major")
    public String showMajorMaster(Model model) {
        model.addAttribute("majorList", adminMajorService.getAllMajors());
        return "admin/mdMajor";
    }

    @PostMapping("/master/major/save")
    public String saveMajorMaster(
            @RequestParam(name = "majorId", required = false) List<Integer> majorIds,
            @RequestParam(name = "majorName", required = false) List<String> majorNames,
            RedirectAttributes redirectAttributes) {
        try {
            adminMajorService.saveMajorList(majorIds, majorNames);
            redirectAttributes.addFlashAttribute("successMessage", "学科情報を保存しました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
        }
        return "redirect:/admin/master/major";
    }

    // --- Tab 2: コースマスタ ---
    @GetMapping("/master/course")
    public String showCourseMaster(Model model) {
        model.addAttribute("courseList", adminCourseService.getAllCourses());
        model.addAttribute("majorList", adminCourseService.getAllMajors());
        return "admin/mdCourse";
    }

    @PostMapping("/master/course/save")
    public String saveCourseMaster(
            @RequestParam(name = "courseId", required = false) List<Integer> courseIds,
            @RequestParam(name = "courseName", required = false) List<String> courseNames,
            @RequestParam(name = "majorId", required = false) List<Integer> majorIds,
            RedirectAttributes redirectAttributes) {
        try {
            adminCourseService.saveCourseList(courseIds, courseNames, majorIds);
            redirectAttributes.addFlashAttribute("successMessage", "コース情報を保存しました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
        }
        return "redirect:/admin/master/course";
    }

    // --- Tab 3: 所属(クラス)マスタ ---
    @GetMapping("/master/department")
    public String showDepartmentMaster(Model model) {
        model.addAttribute("departmentList", adminDepartmentService.getAllDepartments());
        model.addAttribute("courseList", adminDepartmentService.getAllCourses());
        return "admin/mdDepartment";
    }

    @PostMapping("/master/department/save")
    public String saveDepartmentMaster(
            @RequestParam(name = "departmentId", required = false) List<Integer> departmentIds,
            @RequestParam(name = "className", required = false) List<String> classNames,
            @RequestParam(name = "courseId", required = false) List<Integer> courseIds,
            RedirectAttributes redirectAttributes) {
        try {
            adminDepartmentService.saveDepartmentList(departmentIds, classNames, courseIds);
            redirectAttributes.addFlashAttribute("successMessage", "所属情報を保存しました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
        }
        return "redirect:/admin/master/department";
    }
}

