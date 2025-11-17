package com.example.attendancemanagementsystem.student.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // トランザクションを追加

//common パスからインポート
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
    private final EnrollmentsRepository enrollmentsRepository;
    private final TimetableRepository timetableRepository;
    private final AttendanceRepository attendanceRepository;
    private final CalendarRepository calendarRepository;

    @Autowired
    public StudentService(UsersRepository usersRepository,
                          StudentRepository studentRepository,
                          EnrollmentsRepository enrollmentsRepository,
                          TimetableRepository timetableRepository,
                          AttendanceRepository attendanceRepository,
                          CalendarRepository calendarRepository) {
        this.usersRepository = usersRepository;
        this.studentRepository = studentRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.timetableRepository = timetableRepository;
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
        System.out.println("--- StudentService.getStudentHomeData() が呼ばれました ---");
        System.out.println("受け取った loginId: " + loginId);
        System.out.println("受け取った month: " + month);
        
        Map<String, Object> data = new HashMap<>();

        // 1. ログインIDから UsersEntity を取得
        Optional<UsersEntity> usersOpt = usersRepository.findByLoginId(loginId);
        if (usersOpt.isEmpty()) {
            System.out.println("★エラー: usersRepository.findByLoginId でユーザーが見つかりません");
            return data; 
        }
        UsersEntity currentUser = usersOpt.get();
        data.put("studentName", currentUser.getName());
        System.out.println("取得したユーザー名: " + currentUser.getName());
        System.out.println("取得した UserID: " + currentUser.getUserId());

        // 3. カレンダー表示のための日付範囲を計算 (先に移動)
        LocalDate startDate = month.withDayOfMonth(1);
        LocalDate endDate = month.withDayOfMonth(month.lengthOfMonth());
        System.out.println("検索する日付範囲 (startDate): " + startDate);
        System.out.println("検索する日付範囲 (endDate): " + endDate);

        // 4. 【予定】データを取得 (先に移動)
        // (student がいなくても、calendar は取得できるようにする)
        List<CalendarEntity> calendarEvents = calendarRepository.findByUsersAndDateBetween(currentUser, startDate, endDate);
        
        System.out.println("calendarRepository.findByUsersAndDateBetween が実行されました");
        if (calendarEvents.isEmpty()) {
            System.out.println("★結果: 予定は見つかりませんでした (リストは空です)");
        } else {
            System.out.println("★成功: " + calendarEvents.size() + " 件の予定が見つかりました！");
        }
        data.put("calendarEvents", calendarEvents);


        // 2. UsersEntity から StudentEntity を取得 (ロジックを後ろに移動)
        Optional<StudentEntity> studentOpt = studentRepository.findByUsers(currentUser);
        if (studentOpt.isEmpty()) {

            // ★★★ デバッグログを追加 ★★★
            // System.out.println("★注意: studentRepository.findByUsers で生徒情報が見つかりません");
            // student が見つからなくても、カレンダーは表示したいので、ここでは return しない
            
        } else {
            // student が見つかった場合のみ、出欠データを取得
            StudentEntity currentStudent = studentOpt.get();
            System.out.println("StudentEntity が見つかりました。出欠データを取得します。");

            // 5. 【出欠】データを取得
            List<AttendanceEntity> attendances = attendanceRepository.findByStudent(currentStudent);
            data.put("attendanceRecords", attendances);
        }

        return data;
    }
}