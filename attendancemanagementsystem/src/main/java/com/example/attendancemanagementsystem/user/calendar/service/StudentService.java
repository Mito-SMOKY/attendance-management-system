package com.example.attendancemanagementsystem.user.calendar.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.CalendarEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.CalendarRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository; // 追加
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.calendar.dto.AttendanceDto;
import com.example.attendancemanagementsystem.user.calendar.dto.CalendarDto;


@Service
@Transactional(readOnly = true)
public class StudentService {

    private final UsersRepository usersRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final CalendarRepository calendarRepository;
    private final EnrollmentsRepository enrollmentsRepository; // 追加

    public StudentService(UsersRepository usersRepository,
                        StudentRepository studentRepository,
                        AttendanceRepository attendanceRepository,
                        CalendarRepository calendarRepository,
                        EnrollmentsRepository enrollmentsRepository) { // 追加
        this.usersRepository = usersRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.calendarRepository = calendarRepository;
        this.enrollmentsRepository = enrollmentsRepository; // 追加
    }

    /**
     * 生徒のメインメニュー（カレンダー）に必要なデータを取得する
     */
    public Map<String, Object> getStudentHomeData(String loginId, LocalDate month) {
        
        Map<String, Object> data = new HashMap<>();

        // 1. ユーザー取得
        Optional<UsersEntity> usersOpt = usersRepository.findByLoginId(loginId);
        if (usersOpt.isEmpty()) {
            return data; 
        }
        UsersEntity currentUser = usersOpt.get();
        data.put("studentName", currentUser.getName());

        // ★追加: 入学年度(AcademicYear)を取得して画面に渡す
        enrollmentsRepository.findByUserAndIsActiveTrue(currentUser).ifPresent(enrollment -> {
            data.put("academicYear", enrollment.getAcademicYear());
        });

        // 2. 日付範囲
        LocalDate startDate = month.withDayOfMonth(1);
        LocalDate endDate = month.withDayOfMonth(month.lengthOfMonth());

        // 3. 予定(Calendar)取得
        List<CalendarEntity> calendarEntities = calendarRepository.findByUsersAndDateBetween(currentUser, startDate, endDate);
        
        List<CalendarDto> calendarDtos = calendarEntities.stream()
            .map(e -> new CalendarDto(e.getCalendarId(), e.getTitle(), e.getDate()))
            .collect(Collectors.toList());
            
        data.put("calendarEvents", calendarDtos);

        // 4. 出席(Attendance)取得
        Optional<StudentEntity> studentOpt = studentRepository.findByUsers(currentUser);
            
        if (studentOpt.isPresent()) {
            List<AttendanceEntity> attendanceEntities = 
                attendanceRepository.findByStudentAndDateRange(currentUser.getUserId(), startDate, endDate);

            // 日付ごとにグルーピング
            Map<LocalDate, List<AttendanceEntity>> dailyMap = attendanceEntities.stream()
                .filter(a -> a.getSession() != null)
                .collect(Collectors.groupingBy(a -> a.getSession().getSessionDate()));

            List<AttendanceDto> attendanceDtos = new ArrayList<>();

            for (Map.Entry<LocalDate, List<AttendanceEntity>> entry : dailyMap.entrySet()) {
                LocalDate date = entry.getKey();
                List<AttendanceEntity> dailyList = entry.getValue();

                // 時限順(SlotId)に確実にソート
                dailyList.sort((a, b) -> {
                    Integer slotA = (a.getSession().getTimeSlot() != null) ? a.getSession().getTimeSlot().getSlotId() : 0;
                    Integer slotB = (b.getSession().getTimeSlot() != null) ? b.getSession().getTimeSlot().getSlotId() : 0;
                    return slotA.compareTo(slotB);
                });

                String displayStatus = determineDailyStatus(dailyList);
                attendanceDtos.add(new AttendanceDto(date, displayStatus));
            }

            data.put("attendanceRecords", attendanceDtos);
        }

        return data;
    }

    /**
     * 1日の出席リストから、カレンダー表示用のステータス(◎, 〇, △, 欠席)を決定する
     */
    private String determineDailyStatus(List<AttendanceEntity> dailyList) {
        if (dailyList.isEmpty()) return "";

        // ステータス名リストを取得
        List<String> statusNames = dailyList.stream()
            .map(a -> a.getStatus() != null ? a.getStatus().getStatusName() : "")
            .collect(Collectors.toList());

        // --- Step 1: 全欠席チェック ---
        boolean isAllAbsent = statusNames.stream().allMatch(s -> "欠席".equals(s));
        if (isAllAbsent) {
            return "欠席";
        }

        // --- Step 2: 完全出席チェック (◎) ---
        boolean isAllPerfect = statusNames.stream().allMatch(s -> 
               "出席".equals(s) 
            || "公欠".equals(s) 
            || "公欠候補".equals(s) 
            || "出席停止".equals(s)
        );
        
        if (isAllPerfect) {
            return "◎";
        }

        // --- Step 3: 遅刻/早退チェック (△) ---
        String firstStatus = statusNames.get(0);
        String lastStatus = statusNames.get(statusNames.size() - 1);

        boolean startBad = "欠席".equals(firstStatus) || "遅刻".equals(firstStatus);
        boolean endBad = "欠席".equals(lastStatus) || "早退".equals(lastStatus);

        if (startBad || endBad) {
            return "△";
        }

        // --- Step 4: 中抜け (〇) ---
        return "〇";
    }

    @Transactional
    public void addCalendarEvent(String loginId, String title, LocalDate date) {
        UsersEntity user = usersRepository.findByLoginId(loginId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
        
        CalendarEntity newEvent = new CalendarEntity();
        newEvent.setUsers(user);
        newEvent.setTitle(title);
        newEvent.setDate(date);
        calendarRepository.save(newEvent);
    }

    @Transactional
    public void deleteCalendarEvent(@NonNull Integer calendarId) {
        calendarRepository.deleteById(calendarId);
    }
}