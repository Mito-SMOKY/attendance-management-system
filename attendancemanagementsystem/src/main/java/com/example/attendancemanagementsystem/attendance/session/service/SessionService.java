package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.session.dto.SessionDto;
import com.example.attendancemanagementsystem.attendance.session.mapper.SessionDtoMapper;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.ClassroomEntity;
import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimeSlotRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final TimetableRepository timetableRepository;
    private final EnrollmentsRepository enrollmentsRepository;
    private final AttendanceRepository attendanceRepository;
    private final EntryLogService entryLogService;
    private final SessionAttendanceService sessionAttendanceService;
    private final SessionDtoMapper sessionDtoMapper;
    private final TimeSlotRepository timeSlotRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final DepartmentRepository departmentRepository;

    public SessionService(SessionRepository sessionRepository,
                          TimetableRepository timetableRepository,
                          EnrollmentsRepository enrollmentsRepository,
                          AttendanceRepository attendanceRepository,
                          EntryLogService entryLogService,
                          SessionAttendanceService sessionAttendanceService,
                          SessionDtoMapper sessionDtoMapper,
                          TimeSlotRepository timeSlotRepository,
                          SubjectRepository subjectRepository,
                          ClassroomRepository classroomRepository,
                          DepartmentRepository departmentRepository) {
        this.sessionRepository = sessionRepository;
        this.timetableRepository = timetableRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.attendanceRepository = attendanceRepository;
        this.entryLogService = entryLogService;
        this.sessionAttendanceService = sessionAttendanceService;
        this.sessionDtoMapper = sessionDtoMapper;
        this.timeSlotRepository = timeSlotRepository;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * 授業開始処理
     */
    @Transactional
    public SessionEntity startSession(Integer userId, Integer subjectId, LocalDate date, Integer slotId, 
                                      Integer classroomId, Integer departmentId, Integer targetGrade) {
        
        // 重複チェック
        List<SessionEntity> existingSessions = sessionRepository.findByUserIdAndDateAndSlotId(userId, date, slotId);
        if (!existingSessions.isEmpty()) {
            SessionEntity session = existingSessions.get(0);
            if (!session.getSessionFlag()) {
                return session; 
            }
        }
        
        TimeSlotEntity timeSlot = timeSlotRepository.findById(slotId).orElseThrow();
        SubjectEntity subject = subjectRepository.findById(subjectId).orElseThrow();
        ClassroomEntity classroom = classroomRepository.findById(classroomId).orElseThrow();
        DepartmentEntity department = departmentRepository.findById(departmentId).orElseThrow();

        SessionEntity session = new SessionEntity();
        session.setUserId(userId);
        session.setSessionDate(date);
        session.setStartTime(LocalDateTime.now()); // 実測開始時間
        
        session.setTimeSlot(timeSlot);
        session.setSubject(subject);
        session.setClassroom(classroom);
        session.setDepartment(department); 
        session.setTargetGrade(targetGrade);
        session.setSessionFlag(false); 

        timetableRepository.findByUserIdAndDateAndSlotId(userId, date, slotId)
                .ifPresent(session::setTimeTable);

        return sessionRepository.save(session);
    }

    /**
     * 出席者リスト表示
     */
    @Transactional(readOnly = true)
    public List<SessionDto> getSessionAttendees(Integer sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        // 1. 履修生取得
        List<EnrollmentsEntity> enrollments = enrollmentsRepository.findByDepartmentIdAndGrade(
            session.getDepartment().getDepartmentId(), 
            session.getTargetGrade()
        );

        // 2. ログ取得
        List<EntryLogEntity> logs = entryLogService.getLogsForEndSession(session, now);

        // ★追加: 直前の授業（同教室）の出席状況を取得
        Map<Integer, Integer> prevStatusMap = getPreviousSessionStatusMap(session);

        // 3. 判定ロジック
        List<SessionDto> result = new ArrayList<>();
        LocalDateTime baseTime = session.getStartTime();
        LocalDateTime lateBoundary = baseTime.plusMinutes(20);
        LocalDateTime absentBoundary = baseTime.plusMinutes(30);

        for (EnrollmentsEntity enrollment : enrollments) {
            Integer studentId = enrollment.getStudent().getUserId();
            EntryLogEntity myLog = logs.stream()
                .filter(l -> l.getUserId().equals(studentId))
                .findFirst().orElse(null);
            
            SessionDto dto = sessionDtoMapper.toDto(session, enrollment, myLog, null);

            if (myLog != null) {
                if (myLog.getEntryTime().isAfter(absentBoundary)) {
                    dto.setStatusId(2); 
                } else if (myLog.getEntryTime().isAfter(lateBoundary)) {
                    dto.setStatusId(3); 
                } else {
                    dto.setStatusId(1); 
                }
            } else {
                // 連続授業判定 (前回ステータスを参照)
                if (prevStatusMap.containsKey(studentId)) {
                    Integer prevStatus = prevStatusMap.get(studentId);
                    if (prevStatus == 2) {
                        // 前回欠席なら、今回も欠席
                        dto.setStatusId(2);
                        dto.setEntryTime("--:-- --");
                    } else if (prevStatus == 6) {
                        dto.setStatusId(6); // 出席停止
                        dto.setEntryTime("--:-- --");
                    } else {
                        // 前回出席/遅刻なら、連続出席とみなす
                        dto.setStatusId(1); 
                        dto.setEntryTime("(連続)");
                    }
                } else {
                    // 直前の記録なし / 教室が違う -> 欠席
                    dto.setStatusId(2); 
                    dto.setEntryTime("--:-- --");
                }
            }
            result.add(dto);
        }
        return result;
    }

    /**
     * Helper: 直前の授業のStatusMapを取得
     * 条件：同日・同科目・同部門・同日・同教室
     */
    private Map<Integer, Integer> getPreviousSessionStatusMap(SessionEntity currentSession) {
        Map<Integer, Integer> map = new HashMap<>();
        List<SessionEntity> sameDaySessions = sessionRepository.findByDepartmentAndTargetGradeAndSubjectAndSessionDate(
                currentSession.getDepartment(),
                currentSession.getTargetGrade(),
                currentSession.getSubject(),
                currentSession.getSessionDate()
        );
        
        Optional<SessionEntity> prevSessionOpt = sameDaySessions.stream()
                .filter(s -> s.getTimeSlot().getSlotId() < currentSession.getTimeSlot().getSlotId())
                .max(Comparator.comparingInt(s -> s.getTimeSlot().getSlotId()));

        if (prevSessionOpt.isPresent()) {
            SessionEntity prevSession = prevSessionOpt.get();

            // ★教室の一致チェック
            boolean isSameClassroom = prevSession.getClassroom().getClassroomId()
                                        .equals(currentSession.getClassroom().getClassroomId());

            if (isSameClassroom) {
                List<AttendanceEntity> prevAttendances = attendanceRepository.findBySessionId(prevSession.getSessionId());
                for (AttendanceEntity att : prevAttendances) {
                    map.put(att.getStudent().getUserId(), att.getStatus().getStatusId());
                }
            }
        }
        return map;
    }

    /**
     * 授業終了処理
     */
    @Transactional
    public void endSession(Integer sessionId, Map<Integer, Integer> manualChanges) {
        SessionEntity session = sessionRepository.findById(sessionId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        List<EntryLogEntity> logsToProcess = entryLogService.getLogsForEndSession(session, now);
        sessionAttendanceService.registerAttendance(session, logsToProcess, manualChanges);
        entryLogService.markLogsAsProcessed(logsToProcess, now);

        session.setSessionFlag(true); 
        session.setEndTime(now);
        sessionRepository.save(session);
    }

    @Transactional
    public void cancelSession(Integer sessionId) {
        attendanceRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    public SessionEntity getSession(Integer sessionId) {
        return sessionRepository.findById(sessionId).orElseThrow();
    }

    public Optional<SessionEntity> findActiveSessionByUserId(Integer userId) {
        List<SessionEntity> sessions = sessionRepository.findActiveSessionsByUserId(userId);
        return sessions.isEmpty() ? Optional.empty() : Optional.of(sessions.get(0));
    }
}