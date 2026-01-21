package com.example.attendancemanagementsystem.user.admin.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.example.attendancemanagementsystem.common.service.GeminiService;

import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.TimeSlotRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.dto.MdTimetableDto;
import com.example.attendancemanagementsystem.user.admin.service.MdTimetableService;

@Controller
@RequestMapping("/admin/mdTimetable")
public class MdTimetableController {

    @Autowired
    private MdTimetableService mdTimetableService;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private ClassroomRepository classroomRepository;
    @Autowired
    private TimetableRepository timetableRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private GeminiService geminiService;


    /**
     * ★追加: 画像/PDFを受け取ってAIに解析させるAPI
     */
    @PostMapping("/analyze-image")
    @ResponseBody
    public ResponseEntity<String> analyzeImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("[]");
        }
        
        // GeminiServiceを使って解析し、JSON文字列をそのまま返す
        String jsonResult = geminiService.analyzeTimetableImage(file);
        
        return ResponseEntity.ok(jsonResult);
    }

    /**
     * 現在の日付から年度と学期の初期値を設定するヘルパーメソッド
     */
    private void setInitialYearAndTerm(MdTimetableDto dto) {
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        // 日本の学校年度（4月始まり）への対応
        // 1月～3月なら、年度は「去年」になる（例: 2026年1月は2025年度）
        int nendo = (currentMonth < 4) ? currentYear - 1 : currentYear;
        
        // 学期の判定 (4月～9月は前期:1、10月～3月は後期:2)
        int term = (currentMonth >= 4 && currentMonth <= 9) ? 1 : 2;

        if (dto.getYear() == null) dto.setYear(nendo);
        if (dto.getTerm() == null) dto.setTerm(term);
    }

    /**
     * 週間一括登録画面
     */
    @GetMapping
    public String index(Model model) {
        MdTimetableDto dto = new MdTimetableDto();
        // ★修正: 今日の日付から年度を自動セット
        setInitialYearAndTerm(dto);
        
        model.addAttribute("mdTimetableDto", dto);
        setupCommonAttributes(model);

        return "admin/mdTimetable";
    }

    /**
     * 週間一括登録処理
     */
    @PostMapping("/register")
    public String register(@ModelAttribute MdTimetableDto mdTimetableDto, RedirectAttributes redirectAttributes) {
        mdTimetableService.registerWeeklySchedule(mdTimetableDto);
        redirectAttributes.addFlashAttribute("successMessage", "週間スケジュールを一括登録しました。");
        return "redirect:/admin/mdTimetable";
    }

    /**
     * 日別編集画面
     */
    @GetMapping("/daily")
    public String daily(@RequestParam(name = "date", required = false) LocalDate date,
                        @RequestParam(name = "departmentId", required = false) Integer departmentId,
                        Model model) {
        
        MdTimetableDto dto;
        if (date != null && departmentId != null) {
            dto = mdTimetableService.getDailySchedule(departmentId, date);
        } else {
            dto = new MdTimetableDto();
            // 日付指定がない場合は今日
            dto.setStartDate(date != null ? date : LocalDate.now());
            if (departmentId != null) dto.setDepartmentId(departmentId);
        }

        // 共通属性セット
        model.addAttribute("mdTimetableDto", dto);
        setupCommonAttributes(model);

        return "admin/mdTimetableDaily";
    }

    @PostMapping("/daily/update")
    public String updateDaily(@ModelAttribute MdTimetableDto mdTimetableDto, RedirectAttributes redirectAttributes) {
        mdTimetableService.updateDailySchedule(mdTimetableDto);
        redirectAttributes.addFlashAttribute("successMessage", "保存しました！");
        return "redirect:/admin/mdTimetable/daily?date=" + mdTimetableDto.getStartDate() + "&departmentId=" + mdTimetableDto.getDepartmentId();
    }
    
    /**
     * 重複データチェック用API (詳細版)
     */
    @GetMapping("/check-overlap")
    @ResponseBody
    public Map<String, Object> checkOverlap(@RequestParam("departmentId") Integer departmentId,
                                            @RequestParam("startDate") LocalDate startDate,
                                            @RequestParam("endDate") LocalDate endDate) {
        
        Map<String, Object> result = new HashMap<>();
        
        // Repositoryから要約情報を取得
        // 戻り値は Object配列のリスト: [0]=MIN(date), [1]=MAX(date), [2]=COUNT
        List<Object[]> summaryList = timetableRepository.findOverlapSummary(departmentId, startDate, endDate);
        
        if (summaryList != null && !summaryList.isEmpty()) {
            Object[] summary = summaryList.get(0);
            Long count = (Long) summary[2]; // COUNTの結果

            if (count != null && count > 0) {
                result.put("exists", true);
                result.put("minDate", summary[0].toString()); // 最小日付 (例: 2024-04-01)
                result.put("maxDate", summary[1].toString()); // 最大日付
                result.put("count", count);
                return result;
            }
        }

        // データがない場合
        result.put("exists", false);
        return result;
    }

    /**
     * 参照専用画面
     */
    @GetMapping("/view")
    public String view(@RequestParam(name = "departmentId", required = false) Integer departmentId,
                       @RequestParam(name = "date", required = false) LocalDate date,
                       @RequestParam(name = "year", required = false) Integer year,
                       @RequestParam(name = "term", required = false) Integer term,
                       Model model) {
                       
        MdTimetableDto dto = new MdTimetableDto();
        
        // パラメータがあればセット、なければ現在日時から自動計算
        dto.setYear(year);
        dto.setTerm(term);
        setInitialYearAndTerm(dto); // ここでnullなら自動補完される

        // 日付がなければ今日
        if (date == null) date = LocalDate.now();
        dto.setStartDate(date);

        if (departmentId != null) {
            dto.setDepartmentId(departmentId);
            dto = mdTimetableService.getWeeklyScheduleView(departmentId, date);
            
            // サービス実行後も検索条件（年度・学期）を維持
            if (dto.getYear() == null) dto.setYear(year);
            if (dto.getTerm() == null) dto.setTerm(term);
            // ※ setInitialYearAndTermはDTOが空の時用なので、サービス後は再セットが必要な場合がある
            if (dto.getYear() == null) setInitialYearAndTerm(dto);
        }

        model.addAttribute("mdTimetableDto", dto);
        setupCommonAttributes(model);

        return "admin/mdTimetableView";
    }

    // 共通の選択肢データをセットするメソッド
    private void setupCommonAttributes(Model model) {
        model.addAttribute("departmentOptions", mdTimetableService.getDepartmentOptions());
        model.addAttribute("teacherList", mdTimetableService.getTeacherList());
        model.addAttribute("subjectList", subjectRepository.findAll());
        model.addAttribute("classroomList", classroomRepository.findAll());
        model.addAttribute("timeSlots", timeSlotRepository.findAllByOrderBySlotIdAsc());
    }
}