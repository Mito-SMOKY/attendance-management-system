package com.example.attendancemanagementsystem.user.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminStudentListController {

    @GetMapping("/studentList")
    public String showStudentList() {
        return "admin/studentList";
    }

    }
