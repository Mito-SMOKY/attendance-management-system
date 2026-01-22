package com.example.attendancemanagementsystem.attendance.display.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.attendance.display.dto.SubjectAttendanceDto;
import com.example.attendancemanagementsystem.attendance.display.service.SubjectAttendanceService;

@Controller
@RequestMapping("/student")
public class SubjectAttendanceController {

    private final SubjectAttendanceService attendanceService;

    public SubjectAttendanceController(SubjectAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    //1. 画面表示用
    //URL: /student/subject_attendance?subjectId=1
    @GetMapping("/subject_attendance")
    public String showPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("subjectId") Integer subjectId,
            @RequestParam(name = "year", required = false) Integer year,   // パラメータを受け取れるよう追加
            @RequestParam(name = "month", required = false) Integer month, // パラメータを受け取れるよう追加
            Model model) {
        
        LocalDate now = LocalDate.now();
        
        // パラメータが空（初回アクセス）なら現在の年月、あればその値を使用
        int selectedYear = (year != null) ? year : now.getYear();
        int selectedMonth = (month != null) ? month : now.getMonthValue();

        // サービス呼び出しも選択された年月を使用
        SubjectAttendanceDto data = attendanceService.getAttendanceDetails(
                userDetails.getUsername(), subjectId, selectedYear, selectedMonth);

        model.addAttribute("attendanceData", data);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedMonth", selectedMonth);
        
        // 年のリスト生成（現在を基準に前後1年）
        List<Integer> yearList = new ArrayList<>();
        for (int i = now.getYear() - 1; i <= now.getYear() + 1; i++) {
            yearList.add(i); 
        }
        model.addAttribute("yearList", yearList);

        // 月のリスト生成（1〜12）
        List<Integer> monthList = new ArrayList<>();
        for(int i=1; i<=12; i++) monthList.add(i);
        model.addAttribute("monthList", monthList);

        return "student/subjectAttendance"; 
    }
    
    //2. JSからの非同期通信用API
    //URL: /student/api/data?year=2025&month=11&subjectId=1
    @GetMapping("/api/data")
    @ResponseBody
    public SubjectAttendanceDto getApiData(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "subjectId", required = false) Integer subjectId,
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        
        if (subjectId == null) {
            return new SubjectAttendanceDto();
        }

        return attendanceService.getAttendanceDetails(userDetails.getUsername(), subjectId, year, month);
    }

    
}