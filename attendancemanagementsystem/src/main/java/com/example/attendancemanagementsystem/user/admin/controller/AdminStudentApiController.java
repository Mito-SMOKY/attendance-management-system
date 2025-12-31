package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendancemanagementsystem.user.admin.service.AdminStudentListService;

@RestController
@RequestMapping("/admin/api")
public class AdminStudentApiController {

    // ビジネスロジック用Serviceを注入
    @Autowired
    private AdminStudentListService adminStudentService;

    // 生徒一覧取得API
    @GetMapping("/students")
    public Map<String, Object> getStudents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword) {

        // ビジネスロジックは全てServiceに委譲
        return adminStudentService.searchStudents(page, size, keyword);
    }
}