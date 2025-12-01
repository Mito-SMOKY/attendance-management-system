package com.example.attendancemanagementsystem.attendance.display.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.attendance.display.dto.DailyAttendanceDto;
import com.example.attendancemanagementsystem.attendance.display.service.AttendanceDisplayService;

@Controller
@RequestMapping("/student/attendance") // URLを整理 (例: /student/attendance/date)
public class AttendanceDisplayController {

    private final AttendanceDisplayService attendanceDisplayService;

    @Autowired
    public AttendanceDisplayController(AttendanceDisplayService attendanceDisplayService) {
        this.attendanceDisplayService = attendanceDisplayService;
    }

    /**
     * 日付ごとの詳細画面を表示
     * URL例: /student/attendance/date?date=2025-11-14
     */
    @GetMapping("/date")
    public String dateAttendance(@RequestParam("date") String dateStr,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        
        String loginId = userDetails.getUsername();
        LocalDate date = LocalDate.parse(dateStr);

        // 専用Serviceを使用
        List<DailyAttendanceDto> dailyList = attendanceDisplayService.getDailyAttendanceDetails(loginId, date);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd(E)", Locale.JAPANESE);
        String displayDate = date.format(formatter);

        model.addAttribute("dailyList", dailyList);
        model.addAttribute("displayDate", displayDate);

        return "student/date_attendance";
    }
}