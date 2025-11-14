package com.example.attendancemanagementsystem.student.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
public class StudentController {

    /**
     * 生徒用メインメニュー（/student/home）を表示
     */
    @GetMapping("/home")
    public String home() {
        return "student/home"; //src/main/resources/templates/student/home.html を参照
    }
}