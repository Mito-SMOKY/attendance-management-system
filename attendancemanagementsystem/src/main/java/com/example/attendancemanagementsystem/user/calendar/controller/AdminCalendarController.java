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
@RequestMapping("/admin")
public class AdminCalendarController {

    private final AdminCalendarService adminService;

    public AdminCalendarController(AdminCalendarService adminService) {
        this.adminService = adminService;
    }

    /**
     * * @param month "2025-11" のような形式で月の指定を受け取る (オプション)
     */
    @GetMapping("/main_calendar") // (ここは /home から変更されていましたね)
    public String home(Model model, @AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam(required = false) String month) {

        int currentYear = java.time.YearMonth.now().getYear();
        int currentMonth = java.time.YearMonth.now().getMonthValue();

        // 💡 2. モデルに属性名 'year' と 'month' で設定
        model.addAttribute("year", currentYear);
        model.addAttribute("month", currentMonth);

        
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
        Map<String, Object> homeData = adminService.getStudentHomeData(loginId, targetMonth);

        // --- 4. データを Model に詰める ---
        model.addAttribute("studentName", homeData.get("studentName"));
        model.addAttribute("calendarEvents", homeData.get("calendarEvents"));
        model.addAttribute("attendanceRecords", homeData.get("attendanceRecords"));
        model.addAttribute("displayMonth", targetMonth.getYear() + "年 " + targetMonth.getMonthValue() + "月");

        // カレンダー表示に必要な月の情報も渡す
        model.addAttribute("targetMonthDate", targetMonth.toString());
        
        return "admin/main_calendar"; // src/main/resources/templates/student/main_calendar.html を参照
    }

    /**
     * 新しいカレンダー予定を追加する (JSから fetch で呼ばれる)
     * * @param title フォームから送られた "title"
     * @param date  フォームから送られた "date" (YYYY-MM-DD形式)
     */
    @PostMapping("/calendar/add")
    public String addCalendarEvent(@RequestParam String title,
                                   @RequestParam LocalDate date,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        
        // 1. ログイン中のユーザーIDを取得
        String loginId = userDetails.getUsername();

        // 2. サービスを呼び出してDBに保存
        adminService.addCalendarEvent(loginId, title, date);

        // 3. 処理が終わったら、メインメニューにリダイレクトする
        // (JS側は、このリダイレクト指示(response.ok)を受けてページをリロードします)
        return "redirect:/admin/main_calendar";
    }
    // ... (中略)

    /**
    * カレンダー予定を削除する（JSからの DELETEリクエストを受け付ける）
    * @param calendarId 削除対象の予定ID (URLのパスから取得)
    */
    @DeleteMapping("/calendar/delete/{calendarId}")
    public ResponseEntity<Void> deleteCalendarEvent(@PathVariable(required = false) Integer calendarId) {
        
        // 【修正点】 calendarId が null でないかチェック
        // @PathVariable は通常 null にならないが、安全のためチェックし、
        // null の場合は Bad Request (400) を返す
        if (calendarId == null) {
            return ResponseEntity.badRequest().build(); // 400 Bad Request
        }
        
         // サービスを呼び出してDBから削除を実行
        adminService.deleteCalendarEvent(calendarId);

         // 削除成功 (204 No Content)
        return ResponseEntity.noContent().build();
    }


}