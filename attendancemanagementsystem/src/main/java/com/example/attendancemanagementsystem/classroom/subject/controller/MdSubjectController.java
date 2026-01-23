package com.example.attendancemanagementsystem.classroom.subject.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.classroom.subject.service.MdSubjectService;

@Controller
@RequestMapping("/admin")
public class MdSubjectController {

    @Autowired
    private MdSubjectService mdSubjectService;

    // 教科情報マスタ詳細画面 
    @GetMapping("/master/SubjectInformation")
    public String showSubjectInformation(Model model) {

        // テーブル表示用
        model.addAttribute("subjectInfoList", mdSubjectService.getSubjectInfoList());
        
        // ドロップダウン用
        model.addAttribute("teacherList", mdSubjectService.getSimpleTeacherList());       
        model.addAttribute("majorList", mdSubjectService.getSimpleMajorList());           
        model.addAttribute("departmentList", mdSubjectService.getSimpleDepartmentList()); 
        model.addAttribute("gradeList", java.util.Arrays.asList(1, 2, 3)); 
        
        return "admin/mdSubjectInformation";
    }

    // 保存処理 
    @PostMapping("/master/SubjectInformation/save")
    public String saveSubjectInformation(
            @RequestParam(name = "subjectId", required = false) List<Integer> subjectIds,
            @RequestParam(name = "subjectName", required = false) List<String> subjectNames,
            @RequestParam(name = "teacherId", required = false) List<Integer> teacherIds,
            @RequestParam(name = "courseCount", required = false) List<Integer> courseCounts,
            @RequestParam(name = "majorId", required = false) List<Integer> majorIds,
            @RequestParam(name = "departmentId", required = false) List<Integer> departmentIds,
            @RequestParam(name = "grade", required = false) List<Integer> grades,
            RedirectAttributes redirectAttributes) {
        
        try {
            mdSubjectService.saveSubjectList(
                subjectIds,
                subjectNames,
                teacherIds,
                courseCounts,
                majorIds,
                departmentIds,
                grades
            );
            
            redirectAttributes.addFlashAttribute("successMessage", "変更を保存しました。");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "保存中にエラーが発生しました。");
        }

        //リダイレクト先
        return "redirect:/admin/master/SubjectInformation";
    }
}
