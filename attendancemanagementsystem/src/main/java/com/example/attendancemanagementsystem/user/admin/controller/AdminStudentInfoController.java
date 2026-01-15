package com.example.attendancemanagementsystem.user.admin.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.user.admin.dto.StudentAttendanceUpdateDto;
import com.example.attendancemanagementsystem.user.admin.dto.StudentInfoDetailDto;
import com.example.attendancemanagementsystem.user.admin.service.AdminStudentInfoService;

@Controller
@RequestMapping("/admin/student")
public class AdminStudentInfoController {

    private final AdminStudentInfoService studentInfoService;

    public AdminStudentInfoController(AdminStudentInfoService studentInfoService) {
        this.studentInfoService = studentInfoService;
    }

    // 学生の詳細情報表示用エンドポイント
    @GetMapping("/info/{id}")
    public String studentInfo(
            @PathVariable("id") Integer studentId,
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "searchSubject", required = false) String searchSubject,
            Model model) {  
        
        // デフォルトで現在の年月を設定
        if (month == null || month.isEmpty()) {
            month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        StudentInfoDetailDto dto = studentInfoService.getStudentInfo(studentId, month, searchSubject);

        // モデルにデータを追加
        model.addAttribute("student", dto);
        model.addAttribute("currentYearMonth", month);
        model.addAttribute("subjectKeyword", searchSubject);

        return "admin/studentInfo";
    }

    //  出席情報更新用のエンドポイント
    @PostMapping("/student-info/update")
    @ResponseBody
    public String updateAttendance(@RequestBody StudentAttendanceUpdateDto form) {

        //更新処理の呼び出し
        studentInfoService.updateStudentAttendance(form);
        return "OK";
    }
}