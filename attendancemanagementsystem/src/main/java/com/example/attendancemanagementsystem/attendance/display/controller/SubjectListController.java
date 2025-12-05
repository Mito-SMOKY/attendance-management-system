package com.example.attendancemanagementsystem.attendance.display.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.attendancemanagementsystem.attendance.display.dto.SubjectListDto;
import com.example.attendancemanagementsystem.attendance.display.service.SubjectListService;

@Controller
@RequestMapping("/student")
public class SubjectListController {

    private final SubjectListService subjectListService;

    public SubjectListController(SubjectListService subjectListService) {
        this.subjectListService = subjectListService;
    }

    /**
     * 教科一覧画面を表示
     * URL: /student/subject-list
     */
    @GetMapping("/subject-list")
    public String subjectList(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        
        String loginId = userDetails.getUsername();

        List<SubjectListDto> subjectList = subjectListService.getSubjectList(loginId);

        model.addAttribute("subjectList", subjectList);

        return "student/subjectList"; 
    }
}