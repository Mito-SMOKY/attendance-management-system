package com.example.attendancemanagementsystem;

import com.example.attendancemanagementsystem.Dummymodel.TimetableData;
import com.example.attendancemanagementsystem.Dummymodel.TimetableEntry;
import com.example.attendancemanagementsystem.Dummymodel.UserDTO; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@Controller
public class AdminTimetableController {

    private List<UserDTO> getDummyAdminUsers() {
        List<UserDTO> adminUsers = new ArrayList<>();
        // 実際のDBでは、roleがADMINのユーザー（他の管理者）を取得
        adminUsers.add(new UserDTO("admin001", "管理者 A (自身)"));
        adminUsers.add(new UserDTO("admin002", "管理者 B"));
        adminUsers.add(new UserDTO("admin003", "管理者 C"));
        return adminUsers;
    }

    /**
     * [GET /admin/timetable] 時間割画面の初期表示エンドポイント
     */
    @GetMapping("/admin/timetable")
    public String showTimetable(Model model) {
        LocalDate today = LocalDate.now();
        // 当該週の月曜を計算
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        // 管理者ユーザーリスト
        List<UserDTO> adminUsers = getDummyAdminUsers();
        model.addAttribute("adminUsers", adminUsers);

        String defaultUserId = adminUsers.get(0).getId();

        // userId を渡して初期データを取得
        TimetableData initialData = createDummyTimetableData(weekStart, defaultUserId);

        model.addAttribute("weekStart", weekStart.toString());
        model.addAttribute("initialData", initialData);
        model.addAttribute("initialUserId", defaultUserId);
        
        return "admin/timetable";
    }

    /**
     * [GET /api/timetabledata] 週切り替え、年月変更用のデータ取得APIエンドポイント
     */
    @GetMapping("/api/timetabledata")
    @ResponseBody
    public TimetableData getTimetableDataJson(
            @RequestParam("date") String dateStr,
            @RequestParam(value = "userId", defaultValue = "admin001") String userId){
        
        LocalDate weekStart = LocalDate.parse(dateStr);
        System.out.println("API Called for Week starting: " + weekStart + ", User: " + userId);
        
        // ★ userId を渡して時間割データを生成
        return createDummyTimetableData(weekStart, userId);
    }


    // ----------------------------------------------------
    // ★★★ 統合されたダミーデータ生成ロジック ★★★
    // ----------------------------------------------------
    // ★ userId パラメータを必須とする一本化されたメソッド
    private TimetableData createDummyTimetableData(LocalDate weekStart, String userId) {
        TimetableData data = new TimetableData();
        
        // --- 1. 日付情報のセットアップ ---
        data.setWeekStart(weekStart.toString());
        data.setWeekEnd(weekStart.plusDays(6).toString());

        LocalDate middleDay = weekStart.plusDays(3);
        int dayOfMonth = middleDay.getDayOfMonth();
        int weekOfMonthSimple = ((dayOfMonth - 1) / 7) + 1;
        data.setWeekNumber(weekOfMonthSimple);
        List<String> dayNames = List.of("日", "月", "火", "水", "木", "金", "土");

        List<String> dates = new ArrayList<>();
        for (int i = 0; i <= 4; i++) {
            LocalDate date = weekStart.plusDays(i);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            int listIndex = (dayOfWeek == DayOfWeek.SUNDAY) ? 0 : date.getDayOfWeek().getValue();
            String formattedDate = date.getMonthValue() + "/" + date.getDayOfMonth() + dayNames.get(listIndex);
            dates.add(formattedDate);
        }
        data.setDates(dates);

        data.setTimeSlots(List.of(
            "9:30-11:00",
            "11:10-12:30",
            "13:30-14:50",
            "15:00-16:20"
        ));

        // --- 2. スケジュール（時間割）の生成 ---
        int timeSlotCount = data.getTimeSlots().size();
        Map<String, List<TimetableEntry>> schedule = new HashMap<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            String dateKey = date.toString();
            List<TimetableEntry> daySchedule = new ArrayList<>();

            for (int slot = 0; slot < timeSlotCount; slot++) {
                TimetableEntry entry = new TimetableEntry();
                entry.setSubject(null);
                entry.setClassroom(null);

                String subject = null;
                String classroom = null;

                // ★★★ userId に基づいてダミーデータを分岐させる（ロジックを整理） ★★★
                if ("admin001".equals(userId)) { // 管理者 A
                    if (i == 0 && slot == 0) { subject = "会議 A"; classroom = "会議室1"; }
                    if (i == 3 && slot == 1) { subject = "研修会"; classroom = "研修室"; }
                } else if ("admin002".equals(userId)) { // 管理者 B
                    if (i == 1 && slot == 2) { subject = "ミーティング"; classroom = "オンライン"; }
                    if (i == 4 && slot == 3) { subject = "事務作業"; classroom = "オフィス"; }
                } else if ("admin003".equals(userId)) { // 管理者 C
                    if (i == 2 && slot == 3) { subject = "面談"; classroom = "会議室2"; }
                } else {
                    // ★★★ userId が admin 以外の場合 (以前の学生ダミーデータなど) ★★★
                    if (i == 0 && slot == 1) { subject = "システム構築"; classroom = "231"; }
                    if (i == 2 && slot == 2) { subject = "Python II"; classroom = "231"; }
                    if (i == 4 && slot == 0) { subject = "HR"; classroom = "各クラス"; }
                }
                
                if (subject != null) {
                    entry.setSubject(subject);
                    entry.setClassroom(classroom);
                }

                daySchedule.add(entry);
            }
            schedule.put(dateKey, daySchedule);
        }

        data.setSchedule(schedule);
        return data;
    }
}