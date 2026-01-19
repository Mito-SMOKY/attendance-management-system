package com.example.attendancemanagementsystem.user.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.user.admin.dto.StudentSubjectDetailDto;
import com.example.attendancemanagementsystem.user.admin.service.AdminStudentSubjectService;

@Controller
@RequestMapping("/admin")
public class AdminStudentSubjectController {

    @Autowired
    private AdminStudentSubjectService studentSubjectService;

    // 生徒の科目詳細画面を表示
    @GetMapping("/student/{studentId}/subject/{subjectId}")
    public String showSubjectDetail(
            @PathVariable("studentId") Integer studentId,
            @PathVariable("subjectId") Integer subjectId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "fromDeptId", required = false) Integer fromDeptId,
            @RequestParam(name = "fromGrade", required = false) Integer fromGrade,
            Model model
    ) {
        // サービスからDTOを取得
        StudentSubjectDetailDto detail = studentSubjectService.getSubjectDetail(studentId, subjectId);
        
        // 画面にデータを渡す
        model.addAttribute("detail", detail);

        // 戻るボタンのURLを動的に決定
        String backUrl = "/admin/student/info/" + studentId; 
        
        // 教科詳細から来た場合は、その教科詳細画面へ戻る
        if ("subject".equals(from) && fromDeptId != null && fromGrade != null) {
            backUrl = String.format("/admin/subjectInfo?departmentId=%d&subjectId=%d&grade=%d", 
                    fromDeptId, subjectId, fromGrade);
        }
        
        model.addAttribute("backUrl", backUrl);
        
        return "admin/studentSubjectDetail";
    }
}