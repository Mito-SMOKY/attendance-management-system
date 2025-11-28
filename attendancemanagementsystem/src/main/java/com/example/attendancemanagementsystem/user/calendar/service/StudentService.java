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

import com.example.attendancemanagementsystem.attendance.display.dto.DailyAttendanceDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.CalendarEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.CalendarRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;


@Service
@Transactional(readOnly = true)
public class StudentService {

    // --- 6つのリポジトリを注入 ---
    private final UsersRepository usersRepository;
    private final StudentRepository studentRepository;
    // private final EnrollmentsRepository enrollmentsRepository;
    // private final TimetableRepository timetableRepository;
    private final AttendanceRepository attendanceRepository;
    private final CalendarRepository calendarRepository;

    
    public StudentService(UsersRepository usersRepository,
                          StudentRepository studentRepository,
                          EnrollmentsRepository enrollmentsRepository,
                          TimetableRepository timetableRepository,
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
     * * @param loginId ログイン中のユーザーID
     * @param month   表示対象の月 (例: 2025-11-01)
     * @return 画面に表示するためのデータマップ
     */
    public Map<String, Object> getStudentHomeData(String loginId, LocalDate month) {
        
        Map<String, Object> data = new HashMap<>();

        // 1. ログインIDから UsersEntity を取得
        Optional<UsersEntity> usersOpt = usersRepository.findByLoginId(loginId);
        if (usersOpt.isEmpty()) {
            // ユーザーが見つからなければ空のマップを返す
            return data; 
        }
        UsersEntity currentUser = usersOpt.get();
        data.put("studentName", currentUser.getName());

        // 3. カレンダー表示のための日付範囲を計算
        LocalDate startDate = month.withDayOfMonth(1);
        LocalDate endDate = month.withDayOfMonth(month.lengthOfMonth());

        // 4. 【予定】データを取得
        // (student がいなくても、calendar は取得できるようにする)
        List<CalendarEntity> calendarEvents = calendarRepository.findByUsersAndDateBetween(currentUser, startDate, endDate);
        data.put("calendarEvents", calendarEvents);


        // 2. UsersEntity から StudentEntity を取得
        Optional<StudentEntity> studentOpt = studentRepository.findByUsers(currentUser);
            
        if (studentOpt.isPresent()) {
            StudentEntity currentStudent = studentOpt.get();

            // ★ ここを修正: 全件取得ではなく、期間指定で取得する
            List<AttendanceEntity> attendanceEntities = 
                attendanceRepository.findByStudentAndDateRange(currentStudent, startDate, endDate);

            // Entity -> DTO に変換
            List<DailyAttendanceDto> attendanceDtos = attendanceEntities.stream()
                .map(a -> new DailyAttendanceDto(
                    a.getTimeTable().getDate(),          // TimeTable経由で日付を取得
                    a.getStatus().getStatusName()        // Status経由で名称("出席"など)を取得
                ))
                .collect(Collectors.toList());

            data.put("attendanceRecords", attendanceDtos);
        }
        // (student が見つからなくても、カレンダーは表示したいので else は不要)

        return data;
    }

    /**
     * カレンダーに新しい予定を追加する
     * * @param loginId ログイン中のユーザーID
     * @param title   予定のタイトル
     * @param date    予定の日付
     */
    @Transactional // (readOnly = false) を明示的に設定。これによりデータの変更が可能に。
    public void addCalendarEvent(String loginId, String title, LocalDate date) {
        
        // 1. ログインIDから UsersEntity を検索
        //orElseThrow で、もしユーザーが見つからなければ例外を発生させる
        UsersEntity user = usersRepository.findByLoginId(loginId)
                            .orElseThrow(() -> new RuntimeException("User not found for loginId: " + loginId));
        
        // 2. 新しい予定エンティティ (CalendarEntity) を作成
        CalendarEntity newEvent = new CalendarEntity();
        
        // 3. データをセット
        newEvent.setUsers(user);  // 誰の予定か
        newEvent.setTitle(title); // タイトル
        newEvent.setDate(date);   // 日付

        // 4. DBに保存 (INSERT)
        calendarRepository.save(newEvent);
    }

    /**
     * 【書き込み系】カレンダー予定を削除する
     * @param calendarId 削除対象の予定ID
     */
    @Transactional // (readOnly = false) を明示的に設定。これによりデータの変更が可能に。
    public void deleteCalendarEvent(@NonNull Integer calendarId) {
        // IDを指定して予定を削除する
        calendarRepository.deleteById(calendarId);
    }

}