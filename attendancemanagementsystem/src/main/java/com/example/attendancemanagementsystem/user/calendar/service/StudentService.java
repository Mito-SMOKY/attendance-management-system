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

// ★修正: 正しいDTOをインポート
import com.example.attendancemanagementsystem.user.calendar.dto.AttendanceDto;
import com.example.attendancemanagementsystem.user.calendar.dto.CalendarDto;

// Entityのインポート
import com.example.attendancemanagementsystem.attendance.display.dto.DailyAttendanceDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.CalendarEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;

// Repositoryのインポート
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.CalendarRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;


@Service
@Transactional(readOnly = true)
public class StudentService {

    private final UsersRepository usersRepository;
    private final StudentRepository studentRepository;
    // private final EnrollmentsRepository enrollmentsRepository;
    // private final TimetableRepository timetableRepository;
    private final AttendanceRepository attendanceRepository;
    private final CalendarRepository calendarRepository;

    
    public StudentService(UsersRepository usersRepository,
                          StudentRepository studentRepository,
                          AttendanceRepository attendanceRepository,
                          CalendarRepository calendarRepository) {
        this.usersRepository = usersRepository;
        this.studentRepository = studentRepository;
        // this.enrollmentsRepository = enrollmentsRepository;
        // this.timetableRepository = timetableRepository;
        this.attendanceRepository = attendanceRepository;
        this.calendarRepository = calendarRepository;
    }

    /**
     * 生徒のメインメニュー（カレンダー）に必要なデータを取得する
     * @param loginId ログイン中のユーザーID
     * @param month   表示対象の月
     * @return 画面用データマップ
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

        // 2. 日付範囲
        LocalDate startDate = month.withDayOfMonth(1);
        LocalDate endDate = month.withDayOfMonth(month.lengthOfMonth());

        // 3. 予定(Calendar)取得
        List<CalendarEntity> calendarEntities = calendarRepository.findByUsersAndDateBetween(currentUser, startDate, endDate);
        
        // Entity -> DTO変換
        List<CalendarDto> calendarDtos = calendarEntities.stream()
            .map(e -> new CalendarDto(e.getCalendarId(), e.getTitle(), e.getDate()))
            .collect(Collectors.toList());
            
        data.put("calendarEvents", calendarDtos);

        // 4. 出席(Attendance)取得
        Optional<StudentEntity> studentOpt = studentRepository.findByUsers(currentUser);
            
        if (studentOpt.isPresent()) {
            StudentEntity currentStudent = studentOpt.get();

            List<AttendanceEntity> attendanceEntities = 
                attendanceRepository.findByStudentAndDateRange(currentStudent, startDate, endDate);

            // ★修正: ここで AttendanceDto (日付と状態のみ) を使用
            List<AttendanceDto> attendanceDtos = attendanceEntities.stream()
                .map(a -> new AttendanceDto(
                    a.getTimeTable().getDate(),    // 日付
                    a.getStatus().getStatusName()  // "出席"などの文字
                ))
                .collect(Collectors.toList());

            data.put("attendanceRecords", attendanceDtos);
        }

        return data;
    }

    /**
     * カレンダーに新しい予定を追加する
     */
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

    /**
     * カレンダー予定を削除する
     */
    @Transactional
    public void deleteCalendarEvent(Integer calendarId) {
    @Transactional // (readOnly = false) を明示的に設定。これによりデータの変更が可能に。
    public void deleteCalendarEvent(@NonNull Integer calendarId) {
        // IDを指定して予定を削除する
        calendarRepository.deleteById(calendarId);
    }
}