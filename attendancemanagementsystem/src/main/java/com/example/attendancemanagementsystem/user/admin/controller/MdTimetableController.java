package com.example.attendancemanagementsystem.user.admin.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
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
    private TimeSlotRepository timeSlotRepository;

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