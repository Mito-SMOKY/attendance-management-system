package com.example.attendancemanagementsystem.attendance.display.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.display.dto.SubjectAttendanceDto;
import com.example.attendancemanagementsystem.common.dto.AttendanceMetricsDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectFaculty;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
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
    // 追加: SessionRepository
    private final SessionRepository sessionRepository;

    public SubjectAttendanceService(
            UsersRepository usersRepository,
            StudentRepository studentRepository,
            TimetableRepository timetableRepository,
            AttendanceRepository attendanceRepository,
            SubjectRepository subjectRepository,
            ClassroomRepository classroomRepository,
            EnrollmentsRepository enrollmentsRepository,
            SubjectFacultyRepository subjectFacultyRepository,
            AttendanceCalculationService attendanceCalculationService,
            SessionRepository sessionRepository) {
        this.usersRepository = usersRepository;
        this.studentRepository = studentRepository;
        this.timetableRepository = timetableRepository;
        this.attendanceRepository = attendanceRepository;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.subjectFacultyRepository = subjectFacultyRepository;
        this.attendanceCalculationService = attendanceCalculationService;
        this.sessionRepository = sessionRepository;
    }

    public SubjectAttendanceDto getAttendanceDetails(String loginId, Integer subjectId, Integer year, Integer month) {
        SubjectAttendanceDto dto = new SubjectAttendanceDto();
        dto.setSubjectId(subjectId);

        // 1. ユーザー・生徒特定
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found: " + loginId));
        StudentEntity student = studentRepository.findByUser(user)
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
                .map(sf -> usersRepository.findById(sf.getId().getUserId()).map(UsersEntity::getName).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
            if (teacherNames.isEmpty()) {
                teacherNames = "未定";
            }
        }
        dto.setTeacherName(teacherNames);


        // 4. データ取得
        // 予定（Timetable）
        List<TimetableEntity> timetables = timetableRepository.findBySubjectIdAndDateBetweenOrderByDateAscSlotIdAsc(
                subjectId, startDate, endDate);
        // 実績（Session）
        List<SessionEntity> sessions = sessionRepository.findBySubject_SubjectIdAndSessionDateBetweenOrderBySessionDateAscTimeSlot_SlotIdAsc(
                subjectId, startDate, endDate);
        // 出席情報
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

        // データ処理用のマップ作成
        // 日付 -> (時限 -> SessionEntity)
        Map<LocalDate, Map<Integer, SessionEntity>> sessionMap = sessions.stream()
                .filter(s -> s.getTimeSlot() != null)
                .collect(Collectors.groupingBy(
                        SessionEntity::getSessionDate,
                        Collectors.toMap(
                                s -> s.getTimeSlot().getSlotId(),
                                s -> s,
                                (existing, replacement) -> replacement // 万が一重複があった場合は後勝ち
                        )
                ));

        // 日付 -> (時限 -> TimetableEntity)
        Map<LocalDate, Map<Integer, TimetableEntity>> timetableMap = timetables.stream()
                .filter(t -> t.getSlotId() != null)
                .collect(Collectors.groupingBy(
                        TimetableEntity::getDate,
                        Collectors.toMap(
                                TimetableEntity::getSlotId,
                                t -> t,
                                (existing, replacement) -> replacement
                        )
                ));

        // 出席情報マップ (SessionID -> AttendanceEntity)
        Map<Integer, AttendanceEntity> attendanceMap = attendances.stream()
                .filter(a -> a.getSession() != null)
                .collect(Collectors.toMap(
                        a -> a.getSession().getSessionId(),
                        a -> a,
                        (e, r) -> e
                ));

        // 表示すべき全日付のセットを作成 (予定と実績の和集合)
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(timetableMap.keySet());
        allDates.addAll(sessionMap.keySet());

        List<LocalDate> sortedDates = new ArrayList<>(allDates);
        sortedDates.sort(LocalDate::compareTo);

        // 日別ループ
        for (LocalDate date : sortedDates) {
            List<String> statuses = new ArrayList<>();
            // 初期化 (4コマ分)
            for (int i = 0; i < 4; i++) statuses.add("-");
            
            String classroomName = "-"; // その日の代表教室（最後に見つかったものをセットする簡易ロジック）

            // 1限から4限までループ
            for (int slot = 1; slot <= 4; slot++) {
                int listIndex = slot - 1;

                // 優先順位1: Session (実績)
                SessionEntity session = sessionMap.getOrDefault(date, new HashMap<>()).get(slot);
                
                // 優先順位2: Timetable (予定)
                TimetableEntity timetable = timetableMap.getOrDefault(date, new HashMap<>()).get(slot);

                if (session != null) {
                    // --- 実績データがある場合 ---
                    
                    // 教室名の取得 (Session優先)
                    if (session.getClassroom() != null) {
                        classroomName = session.getClassroom().getClassroomName();
                    }

                    // 出席ステータスの取得 (SessionIDで紐付け)
                    AttendanceEntity att = attendanceMap.get(session.getSessionId());
                    String statusSymbol = "-";

                    if (att != null && att.getStatus() != null) {
                        String sName = att.getStatus().getStatusName();
                        
                        if ("出席".equals(sName)) { statusSymbol = "○"; monthlyPresent++; }
                        else if ("欠席".equals(sName)) { statusSymbol = "✕"; monthlyAbsent++; }
                        else if ("遅刻".equals(sName)) { statusSymbol = "△"; monthlyLate++; }
                        else if ("早退".equals(sName)) { statusSymbol = "△"; monthlyEarlyLeave++; }
                        else if ("公欠".equals(sName)) { statusSymbol = "○"; monthlyOfficial++; }
                        else { statusSymbol = sName; }
                    } else if (session.getSessionFlag()) {
                         // Sessionはあるが出席レコードがない場合 (かつ実施済みフラグがtrueなら欠席扱いなどの仕様によるが、ここでは"-"または適宜調整)
                         // 今回は既存ロジックに合わせてレコードがなければ "-" とします
                    }
                    
                    statuses.set(listIndex, statusSymbol);

                } else if (timetable != null) {
                    // --- 実績はないが予定がある場合 ---
                    
                    // 教室名の取得 (Timetableから)
                    if ("-".equals(classroomName) && timetable.getClassroomId() != null) {
                        // Repository経由ではなくEntity経由で取得するか、IDから取得
                         classroomName = classroomRepository.findById(timetable.getClassroomId())
                                .map(c -> c.getClassroomName()).orElse("-");
                    }
                    
                    // ステータスは予定段階なので "-"
                    statuses.set(listIndex, "-");
                }
            }
            
            // 日毎のデータとして追加
            dailyList.add(new SubjectAttendanceDto.DailyDetail(
                    date.format(dayFormatter),
                    statuses,
                    classroomName
            ));
        }

        dto.setDailyAttendanceList(dailyList);
        // 代表教室のセット (リストが空でなければ最初の要素など、既存ロジックに準拠)
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