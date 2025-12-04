package com.example.attendancemanagementsystem;

import com.example.attendancemanagementsystem.Dummymodel.TimetableData;
import com.example.attendancemanagementsystem.Dummymodel.TimetableEntry;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
@RequestMapping("/student")
public class TimetableController {

    /**
     * 時間割画面の初期表示エンドポイント
     * 現在日を含む週のデータを表示
     */
    @GetMapping("/timetable")
    public String showTimetable(Model model) {
        LocalDate today = LocalDate.now();
        // 当該週の月曜を計算
        // LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        model.addAttribute("weekStart", weekStart.toString()); // 例: "2025-12-01"
        // 必要なら初期データも準備して model.addAttribute("initialData", initialData);
        return "student/timetable";
    }

    /**
     * 週切り替え、年月変更用のデータ取得APIエンドポイント
     * @param dateStr 取得したい週の月曜日（YYYY-MM-DD形式）
     */
    @GetMapping("/api/timetabledata")
    @ResponseBody
    public TimetableData getTimetableDataJson(
            @RequestParam("date") String dateStr) {
        
        LocalDate weekStart = LocalDate.parse(dateStr);
        System.out.println("API Called for Week starting: " + weekStart);
        
        return createDummyTimetableData(weekStart);
    }


    // ----------------------------------------------------
    // ★★★ ダミーデータ生成ロジック ★★★
    // ----------------------------------------------------
    private TimetableData createDummyTimetableData(LocalDate weekStart) {
    TimetableData data = new TimetableData();
    
    // ... (週の開始日、終了日の設定は同じ)
    data.setWeekStart(weekStart.toString());
    data.setWeekEnd(weekStart.plusDays(6).toString());

    LocalDate middleDay = weekStart.plusDays(3); 
    // LocalDate firstOfMonth = middleDay.withDayOfMonth(1);
    int dayOfMonth = middleDay.getDayOfMonth();
    int weekOfMonthSimple = ((dayOfMonth - 1) / 7) + 1;
    data.setWeekNumber(weekOfMonthSimple);
    List<String> dayNames = List.of("日", "月", "火", "水", "木", "金", "土"); 

    List<String> dates = new ArrayList<>();
    for (int i = 0; i <= 4; i++) {
        LocalDate date = weekStart.plusDays(i);

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int listIndex;
        if(dayOfWeek == DayOfWeek.SUNDAY){
            listIndex = 0;
        } else {
            listIndex = date.getDayOfWeek().getValue();
        }

        String formattedDate = date.getMonthValue() + "/" + date.getDayOfMonth() + dayNames.get(listIndex);
        dates.add(formattedDate);
    }
     data.setDates(dates);

        // 時間帯リスト（画像に基づき）
        data.setTimeSlots(List.of(
            "9:30-11:00", 
            "11:10-12:30", 
            "13:30-14:50", 
            "15:00-16:20"
        ));

        int timeSlotCount = data.getTimeSlots().size();
        Map<String, List<TimetableEntry>> schedule = new HashMap<String, List<TimetableEntry>>();
    
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            String dateKey = date.toString();
            List<TimetableEntry> daySchedule = new ArrayList<>();

            // 4時間のデータを作成
            for (int slot = 0; slot < timeSlotCount; slot++) {
                TimetableEntry entry = new TimetableEntry();
                entry.setSubject(null);
                entry.setClassroom(null);

                // 月曜日 (i=0) のダミー
                if (i == 0 && slot == 1) { 
                    entry.setSubject("システム構築");
                    entry.setClassroom("231");
                }
                // 水曜日 (i=2) のダミー
                if (i == 2 && slot == 2) { 
                    entry.setSubject("Python II");
                    entry.setClassroom("231");
                }
                // 金曜日 (i=4) のダミー
                if (i == 4 && slot == 0) { 
                    entry.setSubject("HR");
                    entry.setClassroom("各クラス");
                }
                
                daySchedule.add(entry);
            }
            schedule.put(dateKey, daySchedule);
        }
        
        data.setSchedule(schedule);
        return data;
    }
}