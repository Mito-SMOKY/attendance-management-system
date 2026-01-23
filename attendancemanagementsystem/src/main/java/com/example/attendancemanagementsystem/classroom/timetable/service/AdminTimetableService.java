package com.example.attendancemanagementsystem.classroom.timetable.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.TimeSlotRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
public class AdminTimetableService {

    private final UsersRepository usersRepository;
    private final TimetableRepository timetableRepository;
    private final SessionRepository sessionRepository;
    private final TimeSlotRepository timeSlotRepository;

    public AdminTimetableService(UsersRepository usersRepository,
                                TimetableRepository timetableRepository,
                                SessionRepository sessionRepository,
                                TimeSlotRepository timeSlotRepository) {
        this.usersRepository = usersRepository;
        this.timetableRepository = timetableRepository;
        this.sessionRepository = sessionRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    // 管理者権限を持つ全ユーザを取得
    public List<UsersEntity> getAllAdmins() {
        return usersRepository.findByUserTypeId(2);
    }

    // 指定された日付とユーザIDに基づき、一週間分の時間割データを作成
    public Map<String, Object> getTimetableData(String dateStr, String targetLoginId) {
        Map<String, Object> result = new HashMap<>();

        // 日付文字列を解析し、無効な場合は現在日付を基準日に設定
        LocalDate refDate;
        try {
            if (dateStr != null && dateStr.matches("\\d{8}")) {
                refDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } else if (dateStr != null && dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                refDate = LocalDate.parse(dateStr);
            } else {
                refDate = LocalDate.now();
            }
        } catch (Exception e) {
            refDate = LocalDate.now();
        }

        // 対象ユーザを取得し、存在しない場合は空の結果を返却
        UsersEntity targetUser = usersRepository.findByLoginId(targetLoginId).orElse(null);
        if (targetUser == null) {
            return result;
        }

        // 基準日からその週の月曜と金曜の日付を計算
        LocalDate monday = refDate.minusDays(refDate.getDayOfWeek().getValue() - 1);
        LocalDate friday = monday.plusDays(4);

        result.put("weekStart", monday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        // 全時限情報を取得し、表示用の時刻ラベルリストを作成
        List<TimeSlotEntity> allSlots = timeSlotRepository.findAllByOrderBySlotIdAsc();
        List<String> timeSlotLabels = new ArrayList<>();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("H:mm");
        for (TimeSlotEntity slot : allSlots) {
            String label = "";
            if (slot.getStartTime() != null && slot.getEndTime() != null) {
                label = slot.getStartTime().format(timeFmt) + "-" + slot.getEndTime().format(timeFmt);
            }
            timeSlotLabels.add(label);
        }
        result.put("timeSlots", timeSlotLabels);

        // 対象期間内の予定（時間割）データを取得し、該当週のみフィルタリング
        List<TimetableEntity> allPlans = timetableRepository.findByUserId(targetUser.getUserId());
        List<TimetableEntity> weekPlans = allPlans.stream()
            .filter(t -> t.getDate() != null && !t.getDate().isBefore(monday) && !t.getDate().isAfter(friday))
            .collect(Collectors.toList());

        // 対象期間内の実績（授業実施）データを取得し、該当ユーザ分のみフィルタリング
        List<SessionEntity> allSessions = sessionRepository.findBySessionDateBetween(monday, friday);
        List<SessionEntity> weekSessions = allSessions.stream()
            .filter(s -> s.getUserId() != null && s.getUserId().equals(targetUser.getUserId()))
            .collect(Collectors.toList());

        // 日付ごとのスケジュールを格納するマップを初期化（順序保持のためLinkedHashMap）
        Map<String, List<Map<String, String>>> scheduleMap = new LinkedHashMap<>();
        DateTimeFormatter keyFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 月曜から金曜まで1日ずつループ処理
        for (int i = 0; i < 5; i++) {
            LocalDate targetDate = monday.plusDays(i);
            String targetDateKey = targetDate.format(keyFmt);

            // その日の予定と実績データをリストから抽出
            List<TimetableEntity> dailyPlans = weekPlans.stream()
                .filter(t -> t.getDate().equals(targetDate))
                .collect(Collectors.toList());

            List<SessionEntity> dailyActuals = weekSessions.stream()
                .filter(s -> s.getSessionDate().equals(targetDate))
                .collect(Collectors.toList());

            List<Map<String, String>> dailySlots = new ArrayList<>();

            // 各時限（1限、2限...）ごとに処理を実行
            for (TimeSlotEntity slot : allSlots) {
                int currentPeriod = slot.getSlotId();
                Map<String, String> slotInfo = new HashMap<>();

                // 現在の時限に該当する実績と予定を特定
                SessionEntity actual = dailyActuals.stream()
                    .filter(s -> s.getTimeSlot() != null && s.getTimeSlot().getSlotId() == currentPeriod)
                    .findFirst().orElse(null);

                TimetableEntity plan = dailyPlans.stream()
                    .filter(t -> t.getSlotId() == currentPeriod)
                    .findFirst().orElse(null);

                // 実績がある場合はそれを優先し、なければ予定を設定（ステータスで区別）
                if (actual != null) {
                    setSubjectAndRoom(slotInfo, actual.getSubject(), actual.getClassroom());
                    slotInfo.put("status", "actual"); // 実績あり（色が変わる）
                    slotInfo.put("sessionId", String.valueOf(actual.getSessionId()));
                } else if (plan != null) {
                    setSubjectAndRoom(slotInfo, plan.getSubject(), plan.getClassroom());
                    slotInfo.put("status", "plan"); // 予定のみ
                }

                dailySlots.add(slotInfo);
            }
            scheduleMap.put(targetDateKey, dailySlots);
        }

        result.put("schedule", scheduleMap);
        return result;
    }

    // エンティティから教科名と教室名を安全に抽出してマップに設定
    private void setSubjectAndRoom(Map<String, String> map, Object subjectEntity, Object classroomEntity) {
        if (subjectEntity instanceof com.example.attendancemanagementsystem.common.entity.SubjectEntity) {
            map.put("subject", ((com.example.attendancemanagementsystem.common.entity.SubjectEntity) subjectEntity).getSubjectName());
        } else {
            map.put("subject", "不明な科目");
        }
        if (classroomEntity instanceof com.example.attendancemanagementsystem.common.entity.ClassroomEntity) {
            map.put("classroom", ((com.example.attendancemanagementsystem.common.entity.ClassroomEntity) classroomEntity).getClassroomName());
        } else {
            map.put("classroom", "");
        }
    }
}