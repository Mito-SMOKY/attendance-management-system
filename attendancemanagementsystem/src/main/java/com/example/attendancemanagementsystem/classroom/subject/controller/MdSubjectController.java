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

    @GetMapping("/master/SubjectInformation")
    public String showSubjectInformation(Model model) {
        model.addAttribute("subjectInfoList", mdSubjectService.getSubjectInfoList());
        model.addAttribute("teacherList", mdSubjectService.getSimpleTeacherList());       
        model.addAttribute("majorList", mdSubjectService.getSimpleMajorList());           
        model.addAttribute("departmentList", mdSubjectService.getSimpleDepartmentList()); 
        model.addAttribute("gradeList", java.util.Arrays.asList(1, 2, 3)); 
        return "admin/mdSubjectInformation";
    }

    @PostMapping("/master/SubjectInformation/save")
    public String saveSubjectInformation(
            @RequestParam(name = "subjectId", required = false) List<Integer> subjectIds,
            @RequestParam(name = "subjectName", required = false) List<String> subjectNames,
            @RequestParam(name = "teacherId", required = false) List<Integer> teacherIds,
            @RequestParam(name = "majorId", required = false) List<Integer> majorIds,
            @RequestParam(name = "departmentId", required = false) List<Integer> departmentIds,
            @RequestParam(name = "grade", required = false) List<Integer> grades,
            RedirectAttributes redirectAttributes) {
        
        try {
            List<String> skipped = mdSubjectService.saveSubjectList(
                subjectIds,
                subjectNames,
                teacherIds,
                majorIds,
                departmentIds,
                grades
            );
            
            if (skipped.isEmpty()) {
                redirectAttributes.addFlashAttribute("successMessage", "変更を保存しました。");
            } else {
                String msg = "※以下の教科は時間割で使用中のため削除できませんでした（それ以外は保存されました）: " 
                           + String.join(", ", skipped);
                redirectAttributes.addFlashAttribute("errorMessage", msg);
            }

        } catch (IllegalArgumentException e) {
            // ★重複チェックなどでServiceが投げた意図的なエラーをここでキャッチして表示
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "保存中に予期せぬエラーが発生しました。");
        }

        return "redirect:/admin/master/SubjectInformation";
    }
}