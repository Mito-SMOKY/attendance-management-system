package com.example.attendancemanagementsystem.student.controller;

import java.time.LocalDate; 
import java.util.Map; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // @RequestParam をインポート

// Service パスからインポート
import com.example.attendancemanagementsystem.student.service.StudentService;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * 生徒用メインメニュー（/student/home）を表示
     * * @param month "2025-11" のような形式で月の指定を受け取る (オプション)
     */
    @GetMapping("/main_calendar")
    public String home(Model model, @AuthenticationPrincipal UserDetails userDetails,
                       @RequestParam(required = false) String month) {
        
        // --- ▼▼▼ デバッグログ追加 ▼▼▼ ---
        // System.out.println("--- StudentController.home() が呼ばれました ---");
        
        // 1. Spring Security からログイン中のユーザーID (LoginID) を取得
        String loginId = userDetails.getUsername();

        // 2. 表示対象月を決定
        LocalDate targetMonth;
        if (month != null && !month.isEmpty()) {
            // URLクエリ ( /home?month=2025-11 ) から月を指定
            targetMonth = LocalDate.parse(month + "-01"); 
        } else {
            // 指定がない場合は、今月を表示
            targetMonth = LocalDate.now().withDayOfMonth(1);
        }

        // 3. サービスを呼び出してデータを取得
        Map<String, Object> homeData = studentService.getStudentHomeData(loginId, targetMonth);

        // 4. 取得したデータを Model に詰めて View (HTML) に渡す
        model.addAttribute("studentName", homeData.get("studentName"));
        model.addAttribute("calendarEvents", homeData.get("calendarEvents"));
        model.addAttribute("attendanceRecords", homeData.get("attendanceRecords"));

        // カレンダー表示に必要な月の情報も渡す
        model.addAttribute("displayMonth", targetMonth.getYear() + "年 " + targetMonth.getMonthValue() + "月");
        
        return "student/main_calendar"; // src/main/resources/templates/student/home.html を参照
    }
}