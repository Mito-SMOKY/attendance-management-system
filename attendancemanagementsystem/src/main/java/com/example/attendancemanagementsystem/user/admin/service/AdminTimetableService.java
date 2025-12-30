package com.example.attendancemanagementsystem.user.admin.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
public class AdminTimetableService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private TimetableRepository timetableRepository;

    // 1. 全ての管理者(UserTypeID=2)を取得
    public List<UsersEntity> getAllAdmins() {
        return usersRepository.findByUserTypeId(2);
    }

    // 2. 指定されたユーザーと日付の時間割データを取得・整形して返す
    public Map<String, Object> getTimetableData(String dateStr, String targetLoginId) {
        Map<String, Object> result = new HashMap<>();

        // 日付解析 (エラーハンドリング含む)
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            date = LocalDate.now();
        }

        // 週の月曜日を計算
        LocalDate monday = date.with(DayOfWeek.MONDAY);
        result.put("weekStart", monday.toString());

        // ユーザーID(String) から DBのID(Integer) を特定
        Optional<UsersEntity> userOpt = usersRepository.findByLoginId(targetLoginId);
        if (userOpt.isEmpty()) {
            return result; // ユーザーが見つからない場合は空で返す
        }
        Integer dbUserId = userOpt.get().getUserId();

        // DBから時間割データを取得
        // TimetableRepository に findByUserId(Integer userId) が必要です
        List<TimetableEntity> allData = timetableRepository.findByUserId(dbUserId);

        // 今週分(月～金)のデータに整形
        Map<String, List<Map<String, String>>> scheduleMap = new HashMap<>();
        
        for (int i = 0; i < 5; i++) {
            LocalDate targetDate = monday.plusDays(i);
            String targetDateStr = targetDate.toString();
            
            // その日のデータを抽出
            List<TimetableEntity> dailyData = allData.stream()
                .filter(t -> t.getDate().equals(targetDate))
                .collect(Collectors.toList());
            
            // 1コマ目～4コマ目までをマップ化
            List<Map<String, String>> slots = new ArrayList<>();
            // 4コマ固定とする場合
            for(int period = 1; period <= 4; period++) {
                int p = period;
                TimetableEntity t = dailyData.stream()
                        .filter(e -> e.getSlotId() == p)
                        .findFirst()
                        .orElse(null);
                
                Map<String, String> slotInfo = new HashMap<>();
                if(t != null) {
                    // 科目名
                    if (t.getSubject() != null) {
                        slotInfo.put("subject", t.getSubject().getSubjectName());
                    } else {
                        slotInfo.put("subject", "不明な科目");
                    }

                    // 教室名
                    if (t.getClassroom() != null) {
                        slotInfo.put("classroom", t.getClassroom().getClassroomName());
                    } else {
                        slotInfo.put("classroom", "");
                    }
                }
                slots.add(slotInfo); // 授業がないコマも空のMapを入れて枠を確保
            }
            scheduleMap.put(targetDateStr, slots);
        }

        result.put("schedule", scheduleMap);
        return result;
    }
}