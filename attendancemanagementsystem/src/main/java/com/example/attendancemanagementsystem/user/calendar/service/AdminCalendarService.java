package com.example.attendancemanagementsystem.user.calendar.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.CalendarEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.CalendarRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.calendar.dto.CalendarDto;

@Service
@Transactional(readOnly = true)
public class AdminCalendarService {

    private final UsersRepository usersRepository;
    private final CalendarRepository calendarRepository;

    public AdminCalendarService(UsersRepository usersRepository,
                                CalendarRepository calendarRepository) {
        this.usersRepository = usersRepository;
        this.calendarRepository = calendarRepository;
    }

    /**
     * 管理者のメインカレンダー画面に必要なデータを取得する
     * (自身の予定のみを表示し、出席データは含めない)
     * * @param loginId ログイン中の管理者ID
     * @param month   表示対象の月
     * @return 画面用データマップ
     */
    public Map<String, Object> getAdminHomeData(String loginId, LocalDate month) {
        
        Map<String, Object> data = new HashMap<>();

        // 1. ユーザー(管理者)取得
        Optional<UsersEntity> usersOpt = usersRepository.findByLoginId(loginId);
        if (usersOpt.isEmpty()) {
            return data; 
        }
        UsersEntity currentUser = usersOpt.get();
        data.put("adminName", currentUser.getName());

        // 2. 日付範囲の決定 (その月の1日〜末日)
        LocalDate startDate = month.withDayOfMonth(1);
        LocalDate endDate = month.withDayOfMonth(month.lengthOfMonth());

        // 3. 予定(Calendar)取得
        // 管理者自身が登録した予定のみを取得します
        List<CalendarEntity> calendarEntities = calendarRepository.findByUsersAndDateBetween(currentUser, startDate, endDate);
        
        // Entity -> DTO変換
        List<CalendarDto> calendarDtos = calendarEntities.stream()
            .map(e -> new CalendarDto(e.getCalendarId(), e.getTitle(), e.getDate()))
            .collect(Collectors.toList());
            
        data.put("calendarEvents", calendarDtos);

        return data;
    }

    /**
     * カレンダーに新しい予定を追加する
     */
    @Transactional
    public void addCalendarEvent(String loginId, String title, LocalDate date) {
        UsersEntity user = usersRepository.findByLoginId(loginId)
                            .orElseThrow(() -> new RuntimeException("User not found: " + loginId));
        
        CalendarEntity newEvent = new CalendarEntity();
        newEvent.setUsers(user);
        newEvent.setTitle(title);
        newEvent.setDate(date);
        calendarRepository.save(newEvent);
    }

    /**
     * カレンダー予定を削除する
     */
    @Transactional
    public void deleteCalendarEvent(@NonNull Integer calendarId) {
        calendarRepository.deleteById(calendarId);
    }
}