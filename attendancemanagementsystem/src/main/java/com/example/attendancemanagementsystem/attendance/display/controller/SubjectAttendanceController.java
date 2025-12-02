package com.example.attendancemanagementsystem.attendance.display.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.attendancemanagementsystem.attendance.display.dto.SubjectAttendanceDto; // 名前変更
import com.example.attendancemanagementsystem.attendance.display.service.SubjectAttendanceService; // 名前変更

@Controller
@RequestMapping("/student")
public class SubjectAttendanceController {

    private final SubjectAttendanceService subjectAttendanceService;

    // @Autowired
    public SubjectAttendanceController(SubjectAttendanceService subjectAttendanceService) {
        this.subjectAttendanceService = subjectAttendanceService;
    }

    /**
     * 教科一覧画面を表示
     * URL: /student/subject-list
     */
    @GetMapping("/subject-list")
    public String subjectList(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        
        String loginId = userDetails.getUsername();

        List<SubjectAttendanceDto> subjectList = subjectAttendanceService.getSubjectList(loginId);

        model.addAttribute("subjectList", subjectList);

        return "student/subjectList"; // HTMLファイル名はそのまま
    }
}