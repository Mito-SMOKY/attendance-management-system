package com.example.attendancemanagementsystem;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import org.springframework.ui.Model;


@Controller
@RequestMapping("/student")
public class TestControllerStudent {
    

    @GetMapping("/userDetail")
    public String ShowProfileStudent(Model model) { 
        Map<String, String> userData = Map.of(
            "UserID", "2321010",
            "Name", "川島みゆ",
            "Role", "student",
            "Timezone", "Asia/Tokyo"
        );
        
        List<Map<String, String>> subjectData = Arrays.asList(
            Map.of("SubjectID", "24-3-SE", "SubjectName", "システム開発基礎論"),
            Map.of("SubjectID", "24-3-PR", "SubjectName", "Webプログラミング実践"),
            Map.of("SubjectID", "24-3-DB", "SubjectName", "データベース管理"),
            Map.of("SubjectID", "24-2-SE", "SubjectName", "要件定義と設計"),
            Map.of("SubjectID", "24-2-NW", "SubjectName", "クラウドコンピューティング概論"),
            Map.of("SubjectID", "24-2-OS", "SubjectName", "情報セキュリティ基礎")
        );
        
        model.addAttribute("userData", userData);
        model.addAttribute("subjectData", subjectData);

        return "common/userDetail";
    }

    @GetMapping("/profile")
    public String ShowUserDetailtMain(Model model) {
        Map<String, String> userData = Map.of(
            "UserID", "2321010",
            "Name", "川島みゆ",
            "Role", "student",
            "Timezone", "Asia/Tokyo"
        );

        model.addAttribute("userData", userData);

        return "/common/profile";
    }

}
