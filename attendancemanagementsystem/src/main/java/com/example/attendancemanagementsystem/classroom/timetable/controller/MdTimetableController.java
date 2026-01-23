package com.example.attendancemanagementsystem.classroom.timetable.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.classroom.timetable.dto.MdTimetableDto;
import com.example.attendancemanagementsystem.classroom.timetable.service.MdTimetableService;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimeSlotRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.common.service.GeminiService;

@Controller
@RequestMapping("/admin/mdTimetable")
public class MdTimetableController {

    // 必要なサービスとリポジトリの注入
    @Autowired private MdTimetableService mdTimetableService;
    @Autowired private UsersRepository usersRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private TimetableRepository timetableRepository;
    @Autowired private TimeSlotRepository timeSlotRepository;
    @Autowired private GeminiService geminiService;

    // 時間割画像を解析するAPI
    @PostMapping("/analyze-image")
    @ResponseBody
    public ResponseEntity<String> analyzeImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("[]");
        }
        
        // 画像解析を行い結果を取得
        String jsonResult = geminiService.analyzeTimetableImage(file);
        
        return ResponseEntity.ok(jsonResult);
    }

    // 行事予定表PDFを解析するAPI
    @PostMapping("/analyzePdf")
    @ResponseBody
    public String analyzeSchedulePdf(@RequestParam("file") MultipartFile file, 
                                    @RequestParam("year") Integer year,
                                    @RequestParam("term") Integer term, 
                                    @RequestParam(name = "targetGrade", defaultValue = "1") Integer targetGrade) {
        return geminiService.extractHolidaysFromPdf(file, year, term, targetGrade);
    }

    // 現在の日付に基づいて年度と学期の初期値を設定する処理
    private void setInitialYearAndTerm(MdTimetableDto dto) {
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        // 前年度として扱う
        int nendo = (currentMonth < 4) ? currentYear - 1 : currentYear;
        
        // 4-9月を前期、10-3月を後期として判定
        int term = (currentMonth >= 4 && currentMonth <= 9) ? 1 : 2;

        if (dto.getYear() == null) dto.setYear(nendo);
        if (dto.getTerm() == null) dto.setTerm(term);
    }

    // 週間一括登録画面の表示処理
    @GetMapping
    public String index(Model model) {
        MdTimetableDto dto = new MdTimetableDto();

        // 初期表示時に年度と学期を自動設定
        setInitialYearAndTerm(dto);
        model.addAttribute("mdTimetableDto", dto);
        setupCommonAttributes(model);

        return "admin/mdTimetable";
    }

    // 週間スケジュールの登録実行処理
    @PostMapping("/register")
    public String register(@ModelAttribute MdTimetableDto mdTimetableDto, RedirectAttributes redirectAttributes) {
        mdTimetableService.registerWeeklySchedule(mdTimetableDto);
        redirectAttributes.addFlashAttribute("successMessage", "週間スケジュールを一括登録しました。");
        return "redirect:/admin/mdTimetable";
    }

    // --- 削除専用画面の表示 ---
    @GetMapping("/delete")
    public String showDeleteForm(Model model) {
        // DTOの初期化
        MdTimetableDto dto = new MdTimetableDto();
        
        // 年度の初期値セットなど
        setInitialYearAndTerm(dto); 
        
        model.addAttribute("mdTimetableDto", dto);
        
        // クラスなどのプルダウン用データをセット
        setupCommonAttributes(model); 

        return "admin/mdTimetableDelete";
    }

    // --- 削除実行処理 ---
    @PostMapping("/delete")
    public String executeDelete(@ModelAttribute MdTimetableDto dto, RedirectAttributes redirectAttributes) {
        
        // 必須チェック
        if (dto.getDepartmentId() == null || dto.getStartDate() == null || dto.getEndDate() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "クラスと期間は必須です。");
            return "redirect:/admin/mdTimetable/delete";
        }

        try {
            // Serviceの削除処理を呼ぶ
            mdTimetableService.deleteRangeSchedule(dto);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "削除完了: " + dto.getStartDate() + " ～ " + dto.getEndDate() + " のデータを削除しました。");
                
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "削除中にエラーが発生しました。");
        }

        return "redirect:/admin/mdTimetable/delete";
    }

    // 日別編集画面の表示処理
    @GetMapping("/daily")
    public String daily(@RequestParam(name = "date", required = false) LocalDate date,
                        @RequestParam(name = "departmentId", required = false) Integer departmentId,
                        Model model) {
        
        MdTimetableDto dto;

        // 日付と学科が指定されている場合は既存データを取得
        if (date != null && departmentId != null) {
            dto = mdTimetableService.getDailySchedule(departmentId, date);
        } else {

            // 指定がない場合は新規作成として今日の日付等をセット
            dto = new MdTimetableDto();
            dto.setStartDate(date != null ? date : LocalDate.now());
            if (departmentId != null) dto.setDepartmentId(departmentId);
        }

        model.addAttribute("mdTimetableDto", dto);
        setupCommonAttributes(model);

        return "admin/mdTimetableDaily";
    }

    // 日別スケジュールの更新実行処理
    @PostMapping("/daily/update")
    public String updateDaily(@ModelAttribute MdTimetableDto mdTimetableDto, RedirectAttributes redirectAttributes) {
        mdTimetableService.updateDailySchedule(mdTimetableDto);
        redirectAttributes.addFlashAttribute("successMessage", "保存しました！");
        return "redirect:/admin/mdTimetable/daily?date=" + mdTimetableDto.getStartDate() + "&departmentId=" + mdTimetableDto.getDepartmentId();
    }
    
    // 指定期間・学科における重複データの存在チェックAPI
    @GetMapping("/check-overlap")
    @ResponseBody
    public Map<String, Object> checkOverlap(@RequestParam("departmentId") Integer departmentId,
                                            @RequestParam("startDate") LocalDate startDate,
                                            @RequestParam("endDate") LocalDate endDate) {
        
        Map<String, Object> result = new HashMap<>();
        
        // リポジトリから重複データの要約情報を取得
        List<Object[]> summaryList = timetableRepository.findOverlapSummary(departmentId, startDate, endDate);
        
        if (summaryList != null && !summaryList.isEmpty()) {
            Object[] summary = summaryList.get(0);
            Long count = (Long) summary[2];

            // 重複データが存在する場合、その詳細を返す
            if (count != null && count > 0) {
                result.put("exists", true);
                result.put("minDate", summary[0].toString());
                result.put("maxDate", summary[1].toString());
                result.put("count", count);
                return result;
            }
        }

        // 重複なし
        result.put("exists", false);
        return result;
    }

    // 時間割参照専用画面の表示処理
    @GetMapping("/view")
    public String view(@RequestParam(name = "departmentId", required = false) Integer departmentId,
                    @RequestParam(name = "date", required = false) LocalDate date,
                    @RequestParam(name = "year", required = false) Integer year,
                    @RequestParam(name = "term", required = false) Integer term,
                    Model model) {
                        
        MdTimetableDto dto = new MdTimetableDto();
        
        // 年度と学期を確定
        dto.setYear(year);
        dto.setTerm(term);
        setInitialYearAndTerm(dto);

        // 表示基準日の設定
        if (date == null) {
            if (dto.getTerm() == 1) {

                // 前期なら 4月1日
                date = LocalDate.of(dto.getYear(), 4, 1);
            } else {

                // 後期なら 10月1日
                date = LocalDate.of(dto.getYear(), 10, 1);
            }
        }
        dto.setStartDate(date);

        // 学科指定がある場合、該当週のデータを取得
        if (departmentId != null) {
            dto.setDepartmentId(departmentId);

            // 指定された日付（学期初め）を含む週のデータを取得
            dto = mdTimetableService.getWeeklyScheduleView(departmentId, date);
            
            // 検索条件を再設定
            if (dto.getYear() == null) dto.setYear(year);
            if (dto.getTerm() == null) dto.setTerm(term);
            setInitialYearAndTerm(dto);
        }

        model.addAttribute("mdTimetableDto", dto);
        setupCommonAttributes(model);

        return "admin/mdTimetableView";
    }

    // 画面表示に必要な共通マスタデータをセット
    private void setupCommonAttributes(Model model) {
        model.addAttribute("departmentOptions", mdTimetableService.getDepartmentOptions());
        model.addAttribute("teacherList", mdTimetableService.getTeacherList());
        model.addAttribute("subjectList", subjectRepository.findAll());
        model.addAttribute("classroomList", classroomRepository.findAll());
        model.addAttribute("timeSlots", timeSlotRepository.findAllByOrderBySlotIdAsc());
    }
}