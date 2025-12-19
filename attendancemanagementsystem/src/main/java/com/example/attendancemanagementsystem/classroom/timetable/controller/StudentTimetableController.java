package com.example.attendancemanagementsystem.classroom.timetable.controller;

import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.example.attendancemanagementsystem.classroom.timetable.dto.StudentTimetableDto;
import com.example.attendancemanagementsystem.classroom.timetable.service.StudentTimetableService;

@Controller
@RequestMapping("/student")
public class StudentTimetableController {

    private final StudentTimetableService timetableService;

    public StudentTimetableController(StudentTimetableService timetableService) {
        this.timetableService = timetableService;
    }

    //時間割画面を表示
    //URL: /student/timetable
    @GetMapping("/timetable")
    public String showTimetable(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        
        String loginId = userDetails.getUsername();
        LocalDate today = LocalDate.now();

        // 初期表示用のデータを取得
        StudentTimetableDto initialData = timetableService.getTimetableData(loginId, today);

        // JSに渡す初期データ
        model.addAttribute("weekStart", initialData.getWeekStart());
        model.addAttribute("initialDate", today.toString());
        model.addAttribute("initialData", initialData);

        return "student/timeTable";
    }
}