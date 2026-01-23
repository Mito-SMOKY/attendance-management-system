package com.example.attendancemanagementsystem.attendance.display.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.display.dto.SubjectAttendanceDto;
import com.example.attendancemanagementsystem.common.dto.AttendanceMetricsDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectFaculty;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectFacultyRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.common.service.AttendanceCalculationService;

@Service
@Transactional(readOnly = true)
public class SubjectAttendanceService {

    private final UsersRepository usersRepository;
    private final StudentRepository studentRepository;
    private final TimetableRepository timetableRepository;
    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final EnrollmentsRepository enrollmentsRepository;
    private final SubjectFacultyRepository subjectFacultyRepository;
    private final AttendanceCalculationService attendanceCalculationService;

    public SubjectAttendanceService(
            UsersRepository usersRepository,
            StudentRepository studentRepository,
            TimetableRepository timetableRepository,
            AttendanceRepository attendanceRepository,
            SubjectRepository subjectRepository,
            ClassroomRepository classroomRepository,
            EnrollmentsRepository enrollmentsRepository,
            SubjectFacultyRepository subjectFacultyRepository,
            AttendanceCalculationService attendanceCalculationService) {
        this.usersRepository = usersRepository;
        this.studentRepository = studentRepository;
        this.timetableRepository = timetableRepository;
        this.attendanceRepository = attendanceRepository;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.subjectFacultyRepository = subjectFacultyRepository;
        this.attendanceCalculationService = attendanceCalculationService;
    }

    public SubjectAttendanceDto getAttendanceDetails(String loginId, Integer subjectId, Integer year, Integer month) {
        SubjectAttendanceDto dto = new SubjectAttendanceDto();
        dto.setSubjectId(subjectId);

        // 1. ユーザー・生徒特定
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found: " + loginId));
        StudentEntity student = studentRepository.findByUsers(user)
                .orElseThrow(() -> new RuntimeException("Student not found for user: " + loginId));
        int userId = student.getUserId();

        // 入学年度セット
        EnrollmentsEntity enrollment = enrollmentsRepository.findByUserAndIsActiveTrue(user).orElse(null);
        if (enrollment != null) {
            dto.setAcademicYear(enrollment.getAcademicYear());
        }

        // 2. 対象期間の計算
        YearMonth targetYearMonth;
        if (year == null || month == null || month < 1 || month > 12) {
            targetYearMonth = YearMonth.now();
        } else {
            targetYearMonth = YearMonth.of(year, month);
        }
        
        LocalDate startDate = targetYearMonth.atDay(1);
        LocalDate endDate = targetYearMonth.atEndOfMonth();

        // 3. 基本情報セット
        subjectRepository.findById(subjectId).ifPresent(s -> {
            dto.setSubjectName(s.getSubjectName());
            dto.setRequiredClasses(30); 
            // 初期値として仮置きするが、後で共通ロジックで上書きされる
            dto.setMaxAbsenceClasses(10); 
        });

        // 担当教員取得
        List<SubjectFaculty> facultyList = subjectFacultyRepository.findBySubjectId(subjectId);
        String teacherNames = "未定";
        if (!facultyList.isEmpty()) {
            teacherNames = facultyList.stream()
                .map(sf -> usersRepository.findById(sf.getUserId()).map(UsersEntity::getName).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
            if (teacherNames.isEmpty()) {
                teacherNames = "未定";
            }
        }
        dto.setTeacherName(teacherNames);


        // 4. データ取得
        List<TimetableEntity> timetables = timetableRepository.findBySubjectIdAndDateBetweenOrderByDateAscSlotIdAsc(
                subjectId, startDate, endDate);
        List<AttendanceEntity> attendances = attendanceRepository.findByStudentAndDateRangeAndSubject(
                userId, startDate, endDate, subjectId);

        // --- 月間カウンター ---
        int monthlyPresent = 0;
        int monthlyAbsent = 0;
        int monthlyLate = 0;
        int monthlyEarlyLeave = 0;
        int monthlyOfficial = 0;
        int monthlyPending = 0;

        // 6. 日別詳細リスト作成
        List<SubjectAttendanceDto.DailyDetail> dailyList = new ArrayList<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MM/dd(E)", Locale.JAPANESE);

        // 日付ごとにグルーピング
        Map<LocalDate, List<TimetableEntity>> dailyMap = timetables.stream()
                .collect(Collectors.groupingBy(TimetableEntity::getDate));
        List<Map.Entry<LocalDate, List<TimetableEntity>>> sortedEntries = new ArrayList<>(dailyMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        // 日別ループ
        for (Map.Entry<LocalDate, List<TimetableEntity>> entry : sortedEntries) {
            LocalDate date = entry.getKey();
            List<TimetableEntity> tts = entry.getValue();

            List<String> statuses = new ArrayList<>();
            for (int i = 0; i < 4; i++) statuses.add("-");
            String classroomName = "-";

            for (TimetableEntity tt : tts) {
                if (tt.getClassroomId() != null) {
                    classroomName = classroomRepository.findById(tt.getClassroomId())
                            .map(c -> c.getClassroomName()).orElse("-");
                } else {
                    classroomName = "未定";
                }

                String statusSymbol = "-";
                AttendanceEntity att = attendances.stream()
                        .filter(a -> a.getSession() != null 
                                && a.getSession().getTimeTable() != null 
                                && a.getSession().getTimeTable().getTimeTableId().equals(tt.getTimeTableId()))
                        .findFirst().orElse(null);

                if (att != null && att.getStatus() != null) {
                    String sName = att.getStatus().getStatusName();
                    
                    if ("出席".equals(sName)) { statusSymbol = "○"; monthlyPresent++; }
                    else if ("欠席".equals(sName)) { statusSymbol = "✕"; monthlyAbsent++; }
                    else if ("遅刻".equals(sName)) { statusSymbol = "△"; monthlyLate++; }
                    else if ("早退".equals(sName)) { statusSymbol = "△"; monthlyEarlyLeave++; }
                    else if ("公欠".equals(sName)) { statusSymbol = "○"; monthlyOfficial++; }
                    else { statusSymbol = sName; }
                }
                
                if (tt.getSlotId() != null) {
                    int slotIndex = tt.getSlotId() - 1; 
                    if (slotIndex >= 0 && slotIndex < 4) {
                        statuses.set(slotIndex, statusSymbol);
                    }
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

        dto.setPresentClasses(monthlyPresent);
        dto.setAbsentClasses(monthlyAbsent);
        dto.setLateClasses(monthlyLate);
        dto.setEarlyLeaveClasses(monthlyEarlyLeave);
        dto.setOfficialAbsentClasses(monthlyOfficial);
        dto.setOfficialPendingClasses(monthlyPending);
        
        // 7. 共通ロジックで出席率・残り欠席可能日数を計算してセット
        AttendanceMetricsDto metrics = attendanceCalculationService.calculateAttendanceMetrics(userId, subjectId);

        // 結果のセット
        dto.setCurrentAttendanceRate(metrics.getAttendanceRate());
        dto.setMaxAbsenceClasses(metrics.getRemainingAbsenceDays());

        return dto;
    }
}