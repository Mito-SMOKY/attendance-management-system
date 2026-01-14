package com.example.attendancemanagementsystem.user.admin.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.TimeSlotRepository;
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

    public AdminStudentInfoService(
            StudentRepository studentRepository,
            AttendanceRepository attendanceRepository,
            SessionRepository sessionRepository,
            TimeSlotRepository timeSlotRepository,
            EnrollmentsRepository enrollmentsRepository,
            DepartmentSubjectRepository departmentSubjectRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.departmentSubjectRepository = departmentSubjectRepository;
    }

    @Transactional(readOnly = true)
    public StudentInfoDetailDto getStudentInfo(Integer studentId, String targetMonthStr) {
        StudentInfoDetailDto dto = new StudentInfoDetailDto();

        // 1. 生徒情報の取得
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found id: " + studentId));
        
        dto.setStudentId(student.getUserId());
        UsersEntity user = student.getUsers();
        dto.setName(user != null ? user.getName() : "Unknown");
        dto.setCurrentMonth(targetMonthStr);

        // 2. 時限マスタの取得
        List<TimeSlotEntity> allTimeSlots = timeSlotRepository.findAllByOrderBySlotIdAsc();
        List<String> periodHeaders = allTimeSlots.stream()
                .map(ts -> ts.getSlotId() + "限") 
                .collect(Collectors.toList());
        dto.setPeriodHeaders(periodHeaders);

        // 3. 受講教科リストの取得
        EnrollmentsEntity activeEnrollment = enrollmentsRepository.findByUserAndIsActiveTrue(user)
                .orElse(null);

        List<SubjectSimpleDto> displaySubjects = new ArrayList<>();
        Integer targetDeptId = null;
        
        if (activeEnrollment != null && activeEnrollment.getDepartment() != null) {
            targetDeptId = activeEnrollment.getDepartment().getDepartmentId();
            Integer grade = activeEnrollment.getGrade();

            List<SubjectEntity> subjects = departmentSubjectRepository.findSubjectsByDepartmentIdAndGrade(targetDeptId, grade);
            
            displaySubjects = subjects.stream().map(s -> {
                SubjectSimpleDto sd = new SubjectSimpleDto();
                sd.setSubjectId(s.getSubjectId());
                sd.setSubjectName(s.getSubjectName());
                return sd;
            }).collect(Collectors.toList());
        }

        dto.setSubjectList(displaySubjects);

        // 4. 日付範囲の設定
        YearMonth yearMonth = YearMonth.parse(targetMonthStr.replace("/", "-"));
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 5. 授業実績と出席データの取得
        List<SessionEntity> allSessions = sessionRepository.findBySessionDateBetween(startDate, endDate);
        List<AttendanceEntity> attendances = attendanceRepository.findByStudentIdAndDateBetween(studentId, startDate, endDate);

        // 6. 出席サマリー集計
        AttendanceSummaryDto summary = new AttendanceSummaryDto();
        for (AttendanceEntity att : attendances) {
            String statusName = att.getStatus() != null ? att.getStatus().getStatusName() : "";
            countStatus(summary, statusName);
        }
        dto.setSummary(summary);

        // 7. スケジュール表の作成
        final Integer deptIdFilter = targetDeptId;
        List<LocalDate> activeDates = allSessions.stream()
            .filter(s -> deptIdFilter == null || (s.getDepartment() != null && s.getDepartment().getDepartmentId().equals(deptIdFilter)))
            .map(SessionEntity::getSessionDate)
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        List<DailyScheduleDto> scheduleList = new ArrayList<>();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy / MM / dd (E)", Locale.JAPANESE);

        for (LocalDate date : activeDates) {
            DailyScheduleDto dailyDto = new DailyScheduleDto();
            dailyDto.setDateStr(date.format(dateFmt));
            
            List<PeriodDetailDto> periodList = new ArrayList<>();
            boolean hasAnyClass = false; 

            for (TimeSlotEntity timeSlot : allTimeSlots) {
                int slotId = timeSlot.getSlotId();
                PeriodDetailDto pDto = new PeriodDetailDto();
                pDto.setPeriod(slotId);

                SessionEntity session = findSession(allSessions, date, slotId, deptIdFilter);

                if (session != null) {
                    hasAnyClass = true;
                    pDto.setHasClass(true);
                    
                    if (session.getSubject() != null) {
                        pDto.setSubjectName(session.getSubject().getSubjectName());
                    } else {
                        pDto.setSubjectName("教科不明");
                    }

                    AttendanceEntity att = findAttendanceBySessionId(attendances, session.getSessionId());
                    
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

            // ★ここが抜けていました！作成したリストをDTOにセットします
            dailyDto.setPeriods(periodList);

            boolean isAllAbsent = hasAnyClass && isAllAbsent(periodList);
            dailyDto.setAbsentDay(isAllAbsent);
            
            scheduleList.add(dailyDto);
        }
        dto.setScheduleList(scheduleList);

        return dto;
    }

    // --- Helper Methods ---

    private SessionEntity findSession(List<SessionEntity> list, LocalDate date, int slotId, Integer deptId) {
        return list.stream()
            .filter(s -> s.getSessionDate().equals(date) 
                      && s.getTimeSlot() != null 
                      && s.getTimeSlot().getSlotId() == slotId
                      && (deptId == null || (s.getDepartment() != null && s.getDepartment().getDepartmentId().equals(deptId))))
            .findFirst()
            .orElse(null);
    }

    private AttendanceEntity findAttendanceBySessionId(List<AttendanceEntity> list, Integer sessionId) {
        return list.stream()
            .filter(a -> a.getSession() != null && a.getSession().getSessionId().equals(sessionId))
            .findFirst()
            .orElse(null);
    }

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

    private void countStatus(AttendanceSummaryDto summary, String status) {
        if (status == null) return;
        switch (status) {
            case "出席" -> summary.setAttendanceCount(summary.getAttendanceCount() + 1);
            case "欠席" -> summary.setAbsenceCount(summary.getAbsenceCount() + 1);
            case "遅刻" -> summary.setLateCount(summary.getLateCount() + 1);
            case "早退" -> summary.setEarlyLeaveCount(summary.getEarlyLeaveCount() + 1);
            case "公欠" -> summary.setPublicAbsenceCount(summary.getPublicAbsenceCount() + 1);
            case "出席停止" -> summary.setSuspensionCount(summary.getSuspensionCount() + 1);
        }
    }

    private String convertStatusToIcon(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "出席" -> "○";
            case "欠席" -> "×";
            case "遅刻" -> "△";
            case "早退" -> "早";
            case "公欠" -> "公";
            case "出席停止" -> "停";
            default -> "-";
        };
    }

    private String convertStatusToClass(String status) {
        if (status == null) return "";
        return switch (status) {
            case "出席" -> "bg-present";
            case "欠席" -> "bg-sick";
            case "遅刻" -> "bg-late";
            case "早退" -> "bg-early";
            case "公欠" -> "bg-public";
            case "出席停止" -> "bg-suspend";
            default -> "bg-other";
        };
    }
}