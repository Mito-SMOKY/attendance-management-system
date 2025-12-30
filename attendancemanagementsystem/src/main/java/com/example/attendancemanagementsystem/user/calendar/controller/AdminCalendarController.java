package com.example.attendancemanagementsystem.user.calendar.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendancemanagementsystem.user.calendar.service.AdminCalendarService;

@Controller
@RequestMapping("/admin") // 管理者用なので /admin ベース
public class AdminCalendarController {

    private final AdminCalendarService adminCalendarService;

    public AdminCalendarController(AdminCalendarService adminCalendarService) {
        this.adminCalendarService = adminCalendarService;
    }

    /**
     * 管理者用カレンダー画面（/admin/main_calendar）を表示
     */
    @GetMapping("/main_calendar")
    public String showAdminCalendar(Model model, 
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    @RequestParam(required = false) String month) {

        // 1. カレンダーヘッダー用の年月
        int currentYear = java.time.YearMonth.now().getYear();
        int currentMonth = java.time.YearMonth.now().getMonthValue();
        model.addAttribute("year", currentYear);
        model.addAttribute("month", currentMonth);
        
        // 2. ログインID取得
        String loginId = userDetails.getUsername();

        // 3. 表示対象月を決定
        LocalDate targetMonth;
        if (month != null && !month.isEmpty()) {
            try {
                targetMonth = LocalDate.parse(month + "-01");
            } catch (Exception e) {
                targetMonth = LocalDate.now().withDayOfMonth(1);
            }
        } else {
            targetMonth = LocalDate.now().withDayOfMonth(1);
        }

        // 4. Serviceからデータ取得
        Map<String, Object> homeData = adminCalendarService.getAdminHomeData(loginId, targetMonth);

        // 5. Modelにセット
        model.addAttribute("adminName", homeData.get("adminName"));
        model.addAttribute("calendarEvents", homeData.get("calendarEvents")); // JSで描画に使用
        model.addAttribute("displayMonth", targetMonth.getYear() + "年 " + targetMonth.getMonthValue() + "月");
        model.addAttribute("targetMonthDate", targetMonth.toString());
        
        // 管理者用カレンダーHTMLへ
        return "admin/main_calendar"; 
    }

    /**
     * 予定追加処理 (POST)
     */
    @PostMapping("/calendar/add")
    public String addCalendarEvent(@RequestParam String title,
                                @RequestParam LocalDate date,
                                @AuthenticationPrincipal UserDetails userDetails) {
        
        String loginId = userDetails.getUsername();
        
        // DB登録
        adminCalendarService.addCalendarEvent(loginId, title, date);

        // 完了後はカレンダー画面へリダイレクト
        return "redirect:/admin/main_calendar";
    }

    /**
     * 予定削除処理 (DELETE - JSからのFetch API用)
     */
    @DeleteMapping("/calendar/delete/{calendarId}")
    public ResponseEntity<Void> deleteCalendarEvent(@PathVariable Integer calendarId) {
        
        if (calendarId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        adminCalendarService.deleteCalendarEvent(calendarId);

        return ResponseEntity.noContent().build();
    }
}