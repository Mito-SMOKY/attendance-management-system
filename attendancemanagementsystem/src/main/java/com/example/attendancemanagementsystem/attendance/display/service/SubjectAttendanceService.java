package com.example.attendancemanagementsystem.attendance.display.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.display.dto.SubjectAttendanceDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
@Transactional(readOnly = true)
//教科別出席詳細データを提供するサービスクラス
public class SubjectAttendanceService {

    private final UsersRepository usersRepository;
    private final StudentRepository studentRepository;
    private final TimetableRepository timetableRepository;
    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;

    public SubjectAttendanceService(
            UsersRepository usersRepository,
            StudentRepository studentRepository,
            TimetableRepository timetableRepository,
            AttendanceRepository attendanceRepository,
            SubjectRepository subjectRepository,
            ClassroomRepository classroomRepository) {
        this.usersRepository = usersRepository;
        this.studentRepository = studentRepository;
        this.timetableRepository = timetableRepository;
        this.attendanceRepository = attendanceRepository;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
    }

    //指定した教科・年・月の詳細データを取得する
    public SubjectAttendanceDto getAttendanceDetails(String loginId, Integer subjectId, int year, int month) {
        SubjectAttendanceDto dto = new SubjectAttendanceDto();
        dto.setSubjectId(subjectId);

        // 1. ユーザー・生徒特定
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found: " + loginId));
        StudentEntity student = studentRepository.findByUsers(user)
                .orElseThrow(() -> new RuntimeException("Student not found for user: " + loginId));
        int userId = student.getUserId();

        // 2. 対象期間の計算（カレンダー表示用の月）
        YearMonth targetYearMonth = YearMonth.of(year, month);
        LocalDate startDate = targetYearMonth.atDay(1);
        LocalDate endDate = targetYearMonth.atEndOfMonth();

        // 3. 基本情報セット
        subjectRepository.findById(subjectId).ifPresent(s -> {
            dto.setSubjectName(s.getSubjectName());
            dto.setRequiredClasses(30); 
            dto.setMaxAbsenceClasses(10); 
        });

        // 4. その月の時間割を取得
        List<TimetableEntity> timetables = timetableRepository.findBySubjectIdAndDateBetweenOrderByDateAscSlotIdAsc(
                subjectId, startDate, endDate);

        // 5. その月の出席記録を取得
        List<AttendanceEntity> attendances = attendanceRepository.findByStudentAndDateRangeAndSubject(
                student, startDate, endDate, subjectId);

        // --- 月間カウンター (右上の表用) ---
        int monthlyPresent = 0;
        int monthlyAbsent = 0;
        int monthlyLate = 0;
        int monthlyOfficial = 0;
        int monthlyPending = 0;

        // 6. 日別詳細リストの作成
        List<SubjectAttendanceDto.DailyDetail> dailyList = new ArrayList<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MM/dd(E)", Locale.JAPANESE);

        // 日付ごとに時間割をグルーピング
        Map<LocalDate, List<TimetableEntity>> dailyMap = timetables.stream()
                .collect(Collectors.groupingBy(TimetableEntity::getDate));

        // 日付順に処理
        List<Map.Entry<LocalDate, List<TimetableEntity>>> sortedEntries = new ArrayList<>(dailyMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        // 各日の処理
        for (Map.Entry<LocalDate, List<TimetableEntity>> entry : sortedEntries) {
            LocalDate date = entry.getKey();
            List<TimetableEntity> tts = entry.getValue();

            // 初期化
            List<String> statuses = new ArrayList<>();
            for (int i = 0; i < 4; i++) statuses.add("-");
            // 教室名初期化
            String classroomName = "-";

            // 各時間割ごとにステータスを判定
            for (TimetableEntity tt : tts) {
                classroomName = classroomRepository.findById(tt.getClassroomId())
                        .map(c -> c.getClassroomName()).orElse("-");
                // 教師名セット（最初の1回だけ）
                // 本来ならば担当した教員全てを表示させるが、list化した場合エラーが複雑化したため、今後の修正
                if (dto.getTeacherName() == null) {
                    String tName = usersRepository.findById(tt.getUserId()).map(u -> u.getName()).orElse("-");
                    dto.setTeacherName(tName);
                }

                // 出席状況の判定
                String statusSymbol = "-";
                // 当日の出席記録を検索
                AttendanceEntity att = attendances.stream()
                        .filter(a -> a.getTimeTable().getTimeTableId().equals(tt.getTimeTableId()))
                        .findFirst().orElse(null);

                if (att != null) {
                    String sName = att.getStatus().getStatusName();
                    // 表示用シンボルと月間カウント
                    // ※ここでカウントするのは「その月」の回数
                    if ("出席".equals(sName)) { statusSymbol = "○"; monthlyPresent++; }
                    else if ("欠席".equals(sName)) { statusSymbol = "✕"; monthlyAbsent++; }
                    else if ("遅刻".equals(sName)) { statusSymbol = "△"; monthlyLate++; }
                    else if ("公欠".equals(sName)) { statusSymbol = "○"; monthlyOfficial++; }
                    else { statusSymbol = sName; }
                }
                // スロットIDからインデックスを計算
                int slotIndex = tt.getSlotId() - 1; 
                if (slotIndex >= 0 && slotIndex < 4) {
                    statuses.set(slotIndex, statusSymbol);
                }
            }

            // 日別詳細行を追加
            dailyList.add(new SubjectAttendanceDto.DailyDetail(
                    date.format(dayFormatter),
                    statuses,
                    classroomName
            ));
        }
        // 日別リストをDTOにセット
        dto.setDailyAttendanceList(dailyList);
        dto.setClassroom(dailyList.isEmpty() ? "-" : dailyList.get(0).getClassroom());

        // 1. 各回数: ループで数えた「月単位の回数」をセット
        dto.setPresentClasses(monthlyPresent);
        dto.setAbsentClasses(monthlyAbsent);
        dto.setLateClasses(monthlyLate);
        dto.setOfficialAbsentClasses(monthlyOfficial);
        dto.setOfficialPendingClasses(monthlyPending);

        // 2. 出席率: DBから「全期間」のデータを取得して計算
        int totalPresent  = attendanceRepository.countByStatusTotal(userId, subjectId, 1);
        int totalAbsent   = attendanceRepository.countByStatusTotal(userId, subjectId, 2);
        int totalLate     = attendanceRepository.countByStatusTotal(userId, subjectId, 3);
        int totalOfficial = attendanceRepository.countByStatusTotal(userId, subjectId, 4);

        // 全期間の総コマ数
        int totalAllTime = totalPresent + totalAbsent + totalLate + totalOfficial; 
        
        if (totalAllTime > 0) {
            // 全期間の (出席 + 公欠) ÷ 全期間の総コマ数
            double rate = (double) (totalPresent + totalOfficial) / totalAllTime;
            dto.setCurrentAttendanceRate(rate);
        } else {
            dto.setCurrentAttendanceRate(0.0);
        }

        // 3. 残り欠席可能数: 「全期間」の欠席数を使う
        int maxLimit = dto.getMaxAbsenceClasses();
        // 残り = 基準値 - 全期間の欠席数
        int remaining = maxLimit - totalAbsent;
        
        if (remaining < 0) {
            remaining = 0;
        }
        dto.setMaxAbsenceClasses(remaining);

        return dto;
    }
}