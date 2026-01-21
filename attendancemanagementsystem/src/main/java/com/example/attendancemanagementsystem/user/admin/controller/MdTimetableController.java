package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimeSlotRepository; // ★追加
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.dto.MdTimetableDto;
import com.example.attendancemanagementsystem.user.admin.service.MdTimetableService;

@Controller
@RequestMapping("/admin/mdTimetable")
public class MdTimetableController {

    @Autowired
    private MdTimetableService mdTimetableService;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private SubjectRepository subjectRepository; 
    @Autowired
    private ClassroomRepository classroomRepository; 
    @Autowired
    private UsersRepository usersRepository; 
    @Autowired
    private TimeSlotRepository timeSlotRepository; // ★追加

    @GetMapping
    public String index(Model model) {
        // DTO初期化 (Mapの自動生成ロジックが入っている前提)
        MdTimetableDto dto = new MdTimetableDto();
        model.addAttribute("mdTimetableDto", dto);

        // ★修正箇所: DB(timeslotテーブル)から時限リストを取得して画面に渡す
        // これで 1～4限 だけでなく、DBが増えれば自動で 5限、6限 と増えます
        List<TimeSlotEntity> timeSlots = timeSlotRepository.findAllByOrderBySlotIdAsc();
        model.addAttribute("timeSlots", timeSlots);

        // 1. 学科選択肢
        model.addAttribute("departmentOptions", mdTimetableService.getDepartmentOptions());
        // 2. 教員リスト
        model.addAttribute("teacherList", mdTimetableService.getTeacherList());
        // 3. その他
        model.addAttribute("subjectList", subjectRepository.findAll());
        model.addAttribute("classroomList", classroomRepository.findAll()); 

        return "admin/mdTimetable";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute MdTimetableDto mdTimetableDto) {
        mdTimetableService.registerWeeklySchedule(mdTimetableDto);
        return "redirect:/admin/mdTimetable?success";
    }
}