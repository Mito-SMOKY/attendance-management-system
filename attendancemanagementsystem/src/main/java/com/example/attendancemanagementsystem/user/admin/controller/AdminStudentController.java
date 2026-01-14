package com.example.attendancemanagementsystem.user.admin.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.user.admin.dto.StudentInfoDetailDto;
import com.example.attendancemanagementsystem.user.admin.service.AdminStudentInfoService;

@Controller
@RequestMapping("/admin/student")
public class AdminStudentController {

    private final AdminStudentInfoService studentInfoService;

    // ★修正点1: Lombok(@RequiredArgsConstructor)削除に伴うコンストラクタ追加
    public AdminStudentController(AdminStudentInfoService studentInfoService) {
        this.studentInfoService = studentInfoService;
    }

    /**
     * 生徒詳細画面（出席・時間割ビュー）を表示
     */
    @GetMapping("/info/{id}")
    public String studentInfo(
            @PathVariable("id") Integer studentId, // ★修正点2: Long -> Integer (DB定義に合わせる)
            @RequestParam(name = "month", required = false) String month,
            Model model) {

        // 月指定がない場合は「今月」をデフォルトにする (例: "2024-01")
        if (month == null || month.isEmpty()) {
            month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        // Serviceを使って画面表示用データ(DTO)を構築
        StudentInfoDetailDto dto = studentInfoService.getStudentInfo(studentId, month);

        model.addAttribute("studentInfo", dto);
        model.addAttribute("targetMonth", month);

        return "admin/studentInfo";
    }
}