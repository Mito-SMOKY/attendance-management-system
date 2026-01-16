package com.example.attendancemanagementsystem.user.admin.service;

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

    public List<UsersEntity> getAllAdmins() {
        return usersRepository.findByUserTypeId(2);
    }

    public Map<String, Object> getTimetableData(String dateStr, String targetLoginId) {
        Map<String, Object> result = new HashMap<>();

        // 日付解析
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

        UsersEntity targetUser = usersRepository.findByLoginId(targetLoginId).orElse(null);
        if (targetUser == null) {
            return result;
        }

        // 週の開始・終了
        LocalDate monday = refDate.minusDays(refDate.getDayOfWeek().getValue() - 1);
        LocalDate friday = monday.plusDays(4);

        result.put("weekStart", monday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        // 時限リスト取得
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

        // データ取得
        List<TimetableEntity> allPlans = timetableRepository.findByUserId(targetUser.getUserId());
        List<TimetableEntity> weekPlans = allPlans.stream()
            .filter(t -> t.getDate() != null && !t.getDate().isBefore(monday) && !t.getDate().isAfter(friday))
            .collect(Collectors.toList());

        List<SessionEntity> allSessions = sessionRepository.findBySessionDateBetween(monday, friday);
        List<SessionEntity> weekSessions = allSessions.stream()
            .filter(s -> s.getUserId() != null && s.getUserId().equals(targetUser.getUserId()))
            .collect(Collectors.toList());

        // マッピング
        Map<String, List<Map<String, String>>> scheduleMap = new LinkedHashMap<>();
        DateTimeFormatter keyFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < 5; i++) {
            LocalDate targetDate = monday.plusDays(i);
            String targetDateKey = targetDate.format(keyFmt);

            List<TimetableEntity> dailyPlans = weekPlans.stream()
                .filter(t -> t.getDate().equals(targetDate))
                .collect(Collectors.toList());

            List<SessionEntity> dailyActuals = weekSessions.stream()
                .filter(s -> s.getSessionDate().equals(targetDate))
                .collect(Collectors.toList());

            List<Map<String, String>> dailySlots = new ArrayList<>();

            for (TimeSlotEntity slot : allSlots) {
                int currentPeriod = slot.getSlotId();
                Map<String, String> slotInfo = new HashMap<>();

                SessionEntity actual = dailyActuals.stream()
                    .filter(s -> s.getTimeSlot() != null && s.getTimeSlot().getSlotId() == currentPeriod)
                    .findFirst().orElse(null);

                TimetableEntity plan = dailyPlans.stream()
                    .filter(t -> t.getSlotId() == currentPeriod)
                    .findFirst().orElse(null);

                // ★ここが重要！実績があれば status: actual をセットする
                if (actual != null) {
                    setSubjectAndRoom(slotInfo, actual.getSubject(), actual.getClassroom());
                    slotInfo.put("status", "actual"); // ←これがないと色は変わりません
                    slotInfo.put("sessionId", String.valueOf(actual.getSessionId()));
                } else if (plan != null) {
                    setSubjectAndRoom(slotInfo, plan.getSubject(), plan.getClassroom());
                    slotInfo.put("status", "plan");
                }

                dailySlots.add(slotInfo);
            }
            scheduleMap.put(targetDateKey, dailySlots);
        }

        result.put("schedule", scheduleMap);
        return result;
    }

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