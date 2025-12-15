package com.example.attendancemanagementsystem;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/student")
public class TestControllerStudent {
    

    @GetMapping("/userDetail")
    public String ShowProfileStudent(Model model) { 
        Map<String, String> userData = Map.of(
            "UserID", "2321010",
            "Name", "川島みゆ",
            "Role", "student",
            "Timezone", "Asia/Tokyo",
            "Email", "miyu.kawashima@example.com"
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

    @GetMapping("/privacySetting")
    public String ShowUserDetailtMain(Model model) {
        Map<String, String> userData = Map.of(
            "UserID", "2321010",
            "Name", "川島みゆ",
            "Role", "student",
            "Timezone", "Asia/Tokyo",
            "Email", "miyu.kawashima@example.com"
            
        );

        model.addAttribute("userData", userData);

        

        return "/common/privacySetting";
    }

    @PostMapping("/api/updateName")
    @ResponseBody // JSONを返すため
    public Map<String, Object> updateNameDummy(@RequestBody Map<String, String> requestBody) {
        // DB保存の代わりに、成功レスポンスを返す
        System.out.println("--- ダミーAPIが呼ばれました ---");
        System.out.println("受信した新しい名前: " + requestBody.get("newName"));
        
        // 成功を示すJSONを返す
        return Map.of("status", "success", "message", "データはDBに保存されませんでしたが、成功としてシミュレーションしました。");
    }

}
