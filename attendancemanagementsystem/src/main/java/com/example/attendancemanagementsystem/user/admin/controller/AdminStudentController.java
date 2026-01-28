
package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.user.admin.service.AdminStudentListService;

@Controller
@RequestMapping("/admin")
public class AdminStudentController {

    @Autowired
    private AdminStudentListService adminStudentService;

    //生徒一覧画面の表示
    @GetMapping("/studentList")
    public String showStudentList() {
        return "admin/studentList";
    }

    // 生徒一覧取得API
    @GetMapping("/api/students")
    @ResponseBody
    public Map<String, Object> getStudents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "departmentId", required = false) Integer departmentId,
            @RequestParam(name = "grade", required = false) Integer grade,
            @RequestParam(name = "courseId", required = false) Integer courseId
    ) {
        return adminStudentService.searchStudents(
                page, size, keyword, departmentId, grade, courseId
        );
    }

    // フィルター選択肢取得API
    @GetMapping("/api/search-options")
    @ResponseBody
    public Map<String, Object> getSearchOptions() {
        return adminStudentService.getFilterOptions();
    }
}
