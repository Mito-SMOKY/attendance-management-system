package com.example.attendancemanagementsystem.user.admin.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.AttendanceStatusEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimeSlotRepository;
import com.example.attendancemanagementsystem.common.service.SearchService;
import com.example.attendancemanagementsystem.user.admin.dto.StudentAttendanceUpdateDto;
import com.example.attendancemanagementsystem.user.admin.dto.StudentInfoDetailDto;
import com.example.attendancemanagementsystem.user.admin.dto.StudentInfoDetailDto.AttendanceSummaryDto;
import com.example.attendancemanagementsystem.user.admin.dto.StudentInfoDetailDto.DailyScheduleDto;
import com.example.attendancemanagementsystem.user.admin.dto.StudentInfoDetailDto.PeriodDetailDto;
import com.example.attendancemanagementsystem.user.admin.dto.StudentInfoDetailDto.SubjectSimpleDto;

@Service
public class AdminStudentInfoService {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final SessionRepository sessionRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final EnrollmentsRepository enrollmentsRepository;
    private final DepartmentSubjectRepository departmentSubjectRepository;
    private final AttendanceStatusRepository attendanceStatusRepository;
    private final SubjectRepository subjectRepository;
    private final SearchService searchService;

    public AdminStudentInfoService(
            StudentRepository studentRepository,
            AttendanceRepository attendanceRepository,
            SessionRepository sessionRepository,
            TimeSlotRepository timeSlotRepository,
            EnrollmentsRepository enrollmentsRepository,
            DepartmentSubjectRepository departmentSubjectRepository,
            SubjectRepository subjectRepository,
            AttendanceStatusRepository attendanceStatusRepository,
            SearchService searchService) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.departmentSubjectRepository = departmentSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
        this.searchService = searchService;
    }

    // 学生の詳細情報取得
    @Transactional(readOnly = true)
    public StudentInfoDetailDto getStudentInfo(Integer studentId, String targetMonthStr, String searchSubject) {
        StudentInfoDetailDto dto = new StudentInfoDetailDto();

        // 生徒情報の取得
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found id: " + studentId));
        dto.setStudentId(student.getUserId());
        UsersEntity user = student.getUser();
        dto.setName(user != null ? user.getName() : "Unknown");
        dto.setCurrentMonth(targetMonthStr);

        // 時限マスタの取得
        List<TimeSlotEntity> allTimeSlots = timeSlotRepository.findAllByOrderBySlotIdAsc();
        List<String> periodHeaders = allTimeSlots.stream()
                .map(ts -> ts.getSlotId() + "限") 
                .collect(Collectors.toList());
        dto.setPeriodHeaders(periodHeaders);

        // 在籍情報の取得
        EnrollmentsEntity activeEnrollment = enrollmentsRepository.findByUserAndIsActiveTrue(user)
                .orElse(null);

        // 選択可能な月リストの生成
        List<String> selectableMonths = new ArrayList<>();
        if (activeEnrollment != null && activeEnrollment.getAcademicYear() != null) {
            int startYear = activeEnrollment.getAcademicYear();
            YearMonth startYm = YearMonth.of(startYear, 4);
            YearMonth nowYm = YearMonth.now();
            YearMonth targetYm = YearMonth.parse(targetMonthStr.replace("/", "-"));
            
            YearMonth endYm = nowYm.isAfter(targetYm) ? nowYm : targetYm;

            YearMonth current = endYm;
            while (!current.isBefore(startYm)) {
                selectableMonths.add(current.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                current = current.minusMonths(1);
            }
        } else {
            selectableMonths.add(targetMonthStr);
        }
        dto.setSelectableMonths(selectableMonths);

        // 受講教科リストの取得
        List<SubjectSimpleDto> displaySubjects = new ArrayList<>();
        Integer targetDeptId = null;
        Integer targetGrade = null; 
        
        Specification<SubjectEntity> keywordSpec = searchService.createKeywordSpec(searchSubject, List.of("subjectName"));
        boolean filterSuccess = false;

        // 学科・学年による絞り込み
        if (activeEnrollment != null && activeEnrollment.getDepartment() != null) {
            
            targetDeptId = activeEnrollment.getDepartment().getDepartmentId();
            targetGrade = activeEnrollment.getGrade(); 

            List<SubjectEntity> allowedSubjects = departmentSubjectRepository.findSubjectsByDepartmentIdAndGrade(targetDeptId, targetGrade);
            
            List<Integer> allowedSubjectIds = allowedSubjects.stream()
                    .map(SubjectEntity::getSubjectId)
                    .collect(Collectors.toList());

            if (!allowedSubjectIds.isEmpty()) {
                Specification<SubjectEntity> allowedSpec = (root, query, cb) -> root.get("subjectId").in(allowedSubjectIds);
                Specification<SubjectEntity> finalSpec = keywordSpec.and(allowedSpec);

                List<SubjectEntity> filteredSubjects = subjectRepository.findAll(finalSpec);
                
                if (!filteredSubjects.isEmpty()) {
                    displaySubjects = filteredSubjects.stream().map(s -> {
                        SubjectSimpleDto sd = new SubjectSimpleDto();
                        sd.setSubjectId(s.getSubjectId());
                        sd.setSubjectName(s.getSubjectName());
                        return sd;
                    }).collect(Collectors.toList());
                    filterSuccess = true;
                }
            }
        }

        // 絞り込めない場合（在籍情報がない場合のみ救済措置として全検索）
        if (!filterSuccess && activeEnrollment == null) {
            List<SubjectEntity> allSubjects = subjectRepository.findAll(keywordSpec);
            displaySubjects = allSubjects.stream().map(s -> {
                SubjectSimpleDto sd = new SubjectSimpleDto();
                sd.setSubjectId(s.getSubjectId());
                sd.setSubjectName(s.getSubjectName());
                return sd;
            }).collect(Collectors.toList());
        }
        dto.setSubjectList(displaySubjects);

        // 日付範囲の設定
        YearMonth yearMonth = YearMonth.parse(targetMonthStr.replace("/", "-"));
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 授業実績と出席データの取得
        List<SessionEntity> allSessions = sessionRepository.findBySessionDateBetween(startDate, endDate);
        List<AttendanceEntity> attendances = attendanceRepository.findByStudentIdAndDateBetween(studentId, startDate, endDate);

        // 出席サマリー集計 
        AttendanceSummaryDto summary = new AttendanceSummaryDto();

        Map<LocalDate, List<AttendanceEntity>> groupedByDate = attendances.stream()
            .filter(a -> a.getSession() != null)
            .collect(Collectors.groupingBy(a -> a.getSession().getSessionDate()));

        // 日付ごとに「1日の扱い」を判定
        for (List<AttendanceEntity> dailyAtts : groupedByDate.values()) {
            
            dailyAtts.sort((a, b) -> {
                Integer slotA = a.getSession().getTimeSlot().getSlotId();
                Integer slotB = b.getSession().getTimeSlot().getSlotId();
                return slotA.compareTo(slotB);
            });

            List<String> statusList = dailyAtts.stream()
                    .map(a -> a.getStatus() != null ? a.getStatus().getStatusName() : "")
                    .collect(Collectors.toList());

            if (statusList.stream().allMatch(s -> "欠席".equals(s))) {
                summary.setAbsenceCount(summary.getAbsenceCount() + 1);
                continue;
            }
            if (statusList.stream().allMatch(s -> "公欠".equals(s) || "公欠候補".equals(s))) {
                summary.setPublicAbsenceCount(summary.getPublicAbsenceCount() + 1);
                continue;
            }
            if (statusList.stream().allMatch(s -> "出席停止".equals(s))) {
                summary.setSuspensionCount(summary.getSuspensionCount() + 1);
                continue;
            }

            String firstStatus = statusList.get(0);
            String lastStatus = statusList.get(statusList.size() - 1);

            if ("欠席".equals(firstStatus) || "遅刻".equals(firstStatus)) {
                summary.setLateCount(summary.getLateCount() + 1);
                continue;
            }
            if ("欠席".equals(lastStatus) || "早退".equals(lastStatus)) {
                summary.setEarlyLeaveCount(summary.getEarlyLeaveCount() + 1);
                continue;
            }

            summary.setAttendanceCount(summary.getAttendanceCount() + 1);
        }
        
        dto.setSummary(summary);

        // スケジュール表の作成
        final Integer deptIdFilter = targetDeptId;
        final Integer gradeFilter = targetGrade; 

        // 対象月の授業日を抽出（学科・学年フィルターを適用）
        List<LocalDate> activeDates = allSessions.stream()
            .filter(s -> 
                // 学科のチェック
                (deptIdFilter == null || (s.getDepartment() != null && s.getDepartment().getDepartmentId().equals(deptIdFilter)))
                &&
                // 学年のチェック 
                (gradeFilter == null || (s.getTargetGrade() != null && s.getTargetGrade().equals(gradeFilter)))
            )
            .map(SessionEntity::getSessionDate)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        List<DailyScheduleDto> scheduleList = new ArrayList<>();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy / MM / dd (E)", Locale.JAPANESE);

        // 日付ごとにスケジュールを作成
        for (LocalDate date : activeDates) {
            DailyScheduleDto dailyDto = new DailyScheduleDto();
            dailyDto.setDateStr(date.format(dateFmt));
            
            List<PeriodDetailDto> periodList = new ArrayList<>();
            boolean hasAnyClass = false; 

            // 時限ごとに授業と出席情報をセット
            for (TimeSlotEntity timeSlot : allTimeSlots) {
                int slotId = timeSlot.getSlotId();
                PeriodDetailDto pDto = new PeriodDetailDto();
                pDto.setPeriod(slotId);
                SessionEntity session = findSession(allSessions, date, slotId, deptIdFilter, gradeFilter);

                // セッションが存在する場合は授業があるとみなし、教科名と出席情報をセット
                if (session != null) {
                    hasAnyClass = true;
                    pDto.setHasClass(true);
                    
                    if (session.getSubject() != null) {
                        pDto.setSubjectName(session.getSubject().getSubjectName());
                    } else {
                        pDto.setSubjectName("教科不明");
                    }

                    AttendanceEntity att = findAttendanceBySessionId(attendances, session.getSessionId());
                    
                    // 出席情報がある場合はステータスをセット、ない場合は「-」を表示
                    if (att != null) {
                        String statusName = att.getStatus() != null ? att.getStatus().getStatusName() : "-";
                        pDto.setStatusIcon(convertStatusToIcon(statusName));
                        pDto.setStatusClass(convertStatusToClass(statusName));
                    } else {
                        pDto.setStatusIcon("-");
                    }
                } else {
                    pDto.setHasClass(false);
                }
                periodList.add(pDto);
            }
            dailyDto.setPeriods(periodList);
            boolean isAllAbsent = hasAnyClass && isAllAbsent(periodList);
            dailyDto.setAbsentDay(isAllAbsent);
            scheduleList.add(dailyDto);
        }
        dto.setScheduleList(scheduleList);

        return dto;
    }

    // 出席情報更新処理
    @Transactional
    public void updateStudentAttendance(StudentAttendanceUpdateDto form) {
        Integer studentId = form.getStudentId();
        
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        // フォームからの更新情報をもとに出席情報を更新
        for (StudentAttendanceUpdateDto.DailyUpdateDto update : form.getUpdates()) {
            LocalDate date = update.getDate();
            Integer period = update.getPeriod();
            String statusName = update.getStatus();
            SessionEntity session = sessionRepository.findBySessionDateAndTimeSlotSlotId(date, period);
            
            // セッションが見つからない場合はスキップ
            if (session == null) {
                continue; 
            }

            // ステータスマスタからステータスエンティティを取得
            AttendanceStatusEntity statusEntity = attendanceStatusRepository.findByStatusName(statusName);
            if (statusEntity == null) {
                continue; 
            }

            // 既存の出席情報を検索
            AttendanceEntity attendance = attendanceRepository.findBySessionIdAndStudent_UserId(session.getSessionId(), studentId)
                    .orElse(new AttendanceEntity());

            // 新規作成の場合はセッションと生徒をセット
            if (attendance.getAttendanceId() == null) {
                attendance.setStudent(student);
                attendance.setSession(session);
            }

            attendance.setStatusId(statusEntity);
            attendanceRepository.save(attendance);
        }
    }

    // セッションの検索
    private SessionEntity findSession(List<SessionEntity> list, LocalDate date, int slotId, Integer deptId, Integer grade) {
        return list.stream()
            .filter(s -> s.getSessionDate().equals(date) 
                    && s.getTimeSlot() != null 
                    && s.getTimeSlot().getSlotId() == slotId
                    && (deptId == null || (s.getDepartment() != null && s.getDepartment().getDepartmentId().equals(deptId)))
                    && (grade == null || (s.getTargetGrade() != null && s.getTargetGrade().equals(grade)))
            )
            .findFirst()
            .orElse(null);
    }

    // 出席情報の検索
    private AttendanceEntity findAttendanceBySessionId(List<AttendanceEntity> list, Integer sessionId) {
        return list.stream()
            .filter(a -> a.getSession() != null && a.getSession().getSessionId().equals(sessionId))
            .findFirst()
            .orElse(null);
    }

    // 全欠席日の判定
    private boolean isAllAbsent(List<PeriodDetailDto> periods) {
        for (PeriodDetailDto p : periods) {
            if (p.isHasClass()) {
                if (!"bg-sick".equals(p.getStatusClass())) {
                    return false;
                }
            }
        }
        return true;
    }

    // ステータスをアイコンに変換
    private String convertStatusToIcon(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "出席" -> "○";
            case "欠席" -> "×";
            case "遅刻" -> "△";
            case "早退" -> "早";
            case "公欠" -> "公";
            case "公欠候補" -> "候";
            case "出席停止" -> "停";
            default -> "-";
        };
    }

    // ステータスをCSSクラスに変換
    private String convertStatusToClass(String status) {
        if (status == null) return "";
        return switch (status) {
            case "出席" -> "bg-present";
            case "欠席" -> "bg-sick";
            case "遅刻" -> "bg-late";
            case "早退" -> "bg-early";
            case "公欠" -> "bg-public";
            case "公欠候補" -> "bg-public";
            case "出席停止" -> "bg-suspend";
            default -> "bg-other";
        };
    }
}