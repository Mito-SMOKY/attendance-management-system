package com.example.attendancemanagementsystem.user.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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
            Model model
    ) {
        // サービスからDTOを取得
        StudentSubjectDetailDto detail = studentSubjectService.getSubjectDetail(studentId, subjectId);
        
        // 画面に渡す
        model.addAttribute("detail", detail);
        
        // HTMLテンプレートの場所を指定
        return "admin/studentSubjectDetail";
    }
}