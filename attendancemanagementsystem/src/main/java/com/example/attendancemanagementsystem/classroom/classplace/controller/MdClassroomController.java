package com.example.attendancemanagementsystem.classroom.classplace.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendancemanagementsystem.common.entity.ClassroomEntity;
import com.example.attendancemanagementsystem.classroom.classplace.service.MdClassroomService;

@Controller
@RequestMapping("/admin")
public class MdClassroomController {

    @Autowired
    private MdClassroomService mdClassroomService;

    // 教室マスタ詳細画面 
    @GetMapping("/master/classroom")
    public String showClassroomMaster(Model model) {
        List<ClassroomEntity> list = mdClassroomService.getAllClassrooms();
        model.addAttribute("classroomList", list);
        return "admin/mdClassroom";
    }

    // 教室マスタ保存 
    @PostMapping("/master/classroom/save")
    public String saveClassroomMaster(
            @RequestParam(name = "classroomId", required = false) List<Integer> classroomIds,
            @RequestParam(name = "classroomName", required = false) List<String> classroomNames,
            @RequestParam(name = "macAddress", required = false) List<String> macAddresses,
            RedirectAttributes redirectAttributes) {
        
        try {
            mdClassroomService.saveClassroomList(classroomIds, classroomNames, macAddresses);
            redirectAttributes.addFlashAttribute("successMessage", "教室情報を保存しました。");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "保存に失敗しました: " + e.getMessage());
        }

        return "redirect:/admin/master/classroom";
    }
}
