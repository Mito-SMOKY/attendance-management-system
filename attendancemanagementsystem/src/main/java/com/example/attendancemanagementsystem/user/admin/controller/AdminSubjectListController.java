package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.user.admin.dto.AdminSubjectListDto;
import com.example.attendancemanagementsystem.user.admin.service.AdminSubjectListService;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Controller
@RequestMapping("/admin")
public class AdminSubjectListController {

    private final AdminSubjectListService adminSubjectListService;

    public AdminSubjectListController(AdminSubjectListService adminSubjectListService) {
        this.adminSubjectListService = adminSubjectListService;
    }

    // 画面表示用
    @GetMapping("/subjectList")
    public String showSubjectList(Model model) {
        return "admin/subjectList"; 
    }

    // データ取得
    @GetMapping("/api/subject/list")
    @ResponseBody
    public Map<String, Object> getSubjectListApi(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "grades", required = false) List<Integer> grades,
            @RequestParam(name = "classes", required = false) List<String> classes
            ) {
        
        String loginId = userDetails.getUsername();
        
        // 検索結果を取得
        List<AdminSubjectListDto> subjectList = adminSubjectListService.getTeacherSubjects(loginId, search, grades, classes);

        // フィルタ連動用の選択肢を取得
        Map<String, Object> filterOptions = adminSubjectListService.getAvailableFilterOptions(loginId, grades, classes);
        Map<String, Object> response = new HashMap<>();
        response.put("subjects", subjectList);
        response.put("filterOptions", filterOptions);

        return response;
    }
}