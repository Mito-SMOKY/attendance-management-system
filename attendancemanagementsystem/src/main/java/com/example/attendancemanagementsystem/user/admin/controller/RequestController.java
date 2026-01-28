package com.example.attendancemanagementsystem.user.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.user.admin.service.RequestService;

@Controller
@RequestMapping("/admin/request")
public class RequestController {
    
    @Autowired
    private RequestService requestService;

    // メニュー画面
    @GetMapping("/menu")
    public String showRequestMenu() {
        return "admin/request/requestMain";
    }

    // 生徒選択画面
    @GetMapping("/select")
    public String showStudentSelect(@RequestParam("mode") String mode, Model model) {
        String pageTitle;
        switch (mode) {
            case "delete": pageTitle = "生徒削除 - 生徒選択"; break;
            case "status": pageTitle = "ステータス変更 - 生徒選択"; break;
            case "course": pageTitle = "学科・コース変更 - 生徒選択"; break;
            default:       pageTitle = "生徒選択"; break;
        }
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("mode", mode);
        return "admin/request/studentSelect";
    }


    // 確認画面
    @GetMapping("/confirm")
    public String showConfirm(
            @RequestParam("mode") String mode,
            @RequestParam("ids") List<Integer> ids,
            Model model) {
        
        // 1. タイトルとメッセージの設定
        String pageTitle;
        String subTitle;
        String confirmMessage;

        switch (mode) {
            case "delete":
                pageTitle = "生徒削除";
                subTitle = "生徒情報の削除";
                confirmMessage = "退学した生徒の情報を削除します。";
                break;
            case "status":
                pageTitle = "ステータス変更";
                subTitle = "ステータスの変更";
                confirmMessage = "選択した生徒のステータスを変更します。";
                break;
            case "course":
                pageTitle = "学科・コース変更";
                subTitle = "学科・コースの変更";
                confirmMessage = "選択した生徒の学科・コース情報を更新します。";
                break;
            default:
                pageTitle = "確認";
                subTitle = "情報の確認";
                confirmMessage = "";
        }

        // 2. データの取得と変換 (Serviceに任せることでControllerはスリム化)
        List<StudentEntity> students = requestService.getStudentsByIds(ids);
        List<Map<String, Object>> displayList = requestService.convertToDisplayData(students);

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("subTitle", subTitle);
        model.addAttribute("confirmMessage", confirmMessage);
        model.addAttribute("studentList", displayList);
        model.addAttribute("mode", mode);
        
        return "admin/request/requestConfirm";
    }
}