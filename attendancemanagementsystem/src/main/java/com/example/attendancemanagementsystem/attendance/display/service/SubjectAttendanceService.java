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

    /**
     * 指定した教科・年・月の詳細データを取得する
     */
    public SubjectAttendanceDto getAttendanceDetails(String loginId, Integer subjectId, int year, int month) {
        SubjectAttendanceDto dto = new SubjectAttendanceDto();
        dto.setSubjectId(subjectId);

        // 1. ユーザー・生徒特定
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found: " + loginId));
        StudentEntity student = studentRepository.findByUsers(user)
                .orElseThrow(() -> new RuntimeException("Student not found for user: " + loginId));

        // 2. 対象期間の計算
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

        // --- 集計用変数 ---
        int present = 0;
        int absent = 0;
        int late = 0;
        int official = 0;
        int pending = 0;

        List<SubjectAttendanceDto.DailyDetail> dailyList = new ArrayList<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MM/dd(E)", Locale.JAPANESE);

        // 日付ごとに時間割をグルーピング
        Map<LocalDate, List<TimetableEntity>> dailyMap = timetables.stream()
                .collect(Collectors.groupingBy(TimetableEntity::getDate));

        List<Map.Entry<LocalDate, List<TimetableEntity>>> sortedEntries = new ArrayList<>(dailyMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<LocalDate, List<TimetableEntity>> entry : sortedEntries) {
            LocalDate date = entry.getKey();
            List<TimetableEntity> tts = entry.getValue();

            List<String> statuses = new ArrayList<>();
            // ★修正箇所1: "no-class" を "-" に変更
            for (int i = 0; i < 4; i++) statuses.add("-");
            
            String classroomName = "-";

            for (TimetableEntity tt : tts) {
                classroomName = classroomRepository.findById(tt.getClassroomId())
                        .map(c -> c.getClassroomName()).orElse("-");
                
                if (dto.getTeacherName() == null) {
                     String tName = usersRepository.findById(tt.getUserId()).map(u -> u.getName()).orElse("-");
                     dto.setTeacherName(tName);
                }

                // ★修正箇所2: "no-class" を "-" に変更
                String statusSymbol = "-";
                
                AttendanceEntity att = attendances.stream()
                        .filter(a -> a.getTimeTable().getTimeTableId().equals(tt.getTimeTableId()))
                        .findFirst().orElse(null);

                if (att != null) {
                    String sName = att.getStatus().getStatusName();
                    // カウント処理
                    if ("出席".equals(sName)) { statusSymbol = "○"; present++; }
                    else if ("欠席".equals(sName)) { statusSymbol = "✕"; absent++; }
                    else if ("遅刻".equals(sName)) { statusSymbol = "△"; late++; }
                    else if ("公欠".equals(sName)) { statusSymbol = "○"; official++; }
                    else { statusSymbol = sName; }
                }
                
                int slotIndex = tt.getSlotId() - 1; 
                if (slotIndex >= 0 && slotIndex < 4) {
                    statuses.set(slotIndex, statusSymbol);
                }
            }

            dailyList.add(new SubjectAttendanceDto.DailyDetail(
                    date.format(dayFormatter),
                    statuses,
                    classroomName
            ));
        }
        
        dto.setDailyAttendanceList(dailyList);
        dto.setClassroom(dailyList.isEmpty() ? "-" : dailyList.get(0).getClassroom());

        dto.setPresentClasses(present);
        dto.setAbsentClasses(absent);
        dto.setLateClasses(late);
        dto.setOfficialAbsentClasses(official);
        dto.setOfficialPendingClasses(pending);

        int total = present + absent + late + official; 
        if (total > 0) {
            double rate = (double) (present + official) / total;
            dto.setCurrentAttendanceRate(rate);
        }

        return dto;
    }
}