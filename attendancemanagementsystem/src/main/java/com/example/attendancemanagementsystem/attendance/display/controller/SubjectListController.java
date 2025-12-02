package com.example.attendancemanagementsystem.attendance.display.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.attendancemanagementsystem.attendance.display.dto.SubjectListDto; // 名前変更
import com.example.attendancemanagementsystem.attendance.display.service.SubjectListService; // 名前変更

@Controller
@RequestMapping("/student")
public class SubjectListController {

    private final SubjectListService subjectAttendanceService;

    // @Autowired
    public SubjectListController(SubjectListService subjectAttendanceService) {
        this.subjectAttendanceService = subjectAttendanceService;
    }

    /**
     * 教科一覧画面を表示
     * URL: /student/subject-list
     */
    @GetMapping("/subject-list")
    public String subjectList(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        
        String loginId = userDetails.getUsername();

        List<SubjectListDto> subjectList = subjectAttendanceService.getSubjectList(loginId);

        model.addAttribute("subjectList", subjectList);

        return "student/subjectList"; // HTMLファイル名はそのまま
    }
}