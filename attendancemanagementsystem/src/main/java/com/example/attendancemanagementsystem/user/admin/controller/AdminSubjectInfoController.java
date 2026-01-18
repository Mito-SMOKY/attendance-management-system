package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody; // ★追加

import com.example.attendancemanagementsystem.user.admin.dto.AdminSubjectInfoDto;
import com.example.attendancemanagementsystem.user.admin.service.AdminSubjectInfoService;

@Controller
@RequestMapping("/admin")
public class AdminSubjectInfoController {

    private final AdminSubjectInfoService adminSubjectInfoService;

    public AdminSubjectInfoController(AdminSubjectInfoService adminSubjectInfoService) {
        this.adminSubjectInfoService = adminSubjectInfoService;
    }

    @GetMapping("/subjectInfo")
    public String showSubjectInfo(
            @RequestParam("departmentId") Integer departmentId,
            @RequestParam("subjectId") Integer subjectId,
            @RequestParam("grade") Integer grade,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "fromStudentId", required = false) Integer fromStudentId,
            Model model) {

        AdminSubjectInfoDto subjectInfo = adminSubjectInfoService.getSubjectInfo(departmentId, subjectId, grade);
        model.addAttribute("info", subjectInfo);

        String backUrl = "/admin/subjectList";
        
        // 生徒詳細から来た場合は、その生徒詳細へ戻る
        if ("student".equals(from) && fromStudentId != null) {
            backUrl = "/admin/student/info/" + fromStudentId;
        }
        model.addAttribute("backUrl", backUrl);
        
        return "admin/subjectInfo"; 
    }

    // --- ★追加: 検索API (JSONを返す) ---
    @GetMapping("/api/subject/search")
    @ResponseBody
    public List<AdminSubjectInfoDto.SubjectOption> searchSubjects(@RequestParam("q") String query) {
        return adminSubjectInfoService.searchSubjects(query);
    }
}