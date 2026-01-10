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

    // 授業開始処理
    @Transactional
    public SessionEntity startSession(Integer userId, Integer subjectId, LocalDate date, Integer slotId, 
                                    Integer classroomId, Integer departmentId, Integer targetGrade) {
        
        //実施中の授業を取得
        List<SessionEntity> activeSessions = sessionRepository.findActiveSessionsByUserId(userId);
        
        // 実施中の授業がある場合のチェック処理
        for (SessionEntity active : activeSessions) {

            // 違う時限の授業が進行中かチェック
            if (!active.getTimeSlot().getSlotId().equals(slotId)) {
                throw new IllegalStateException("終了していない他時限(" + active.getTimeSlot().getSlotId() + "限)の授業があります。先に終了させてください。");
            }

            // 同じ時限・同じクラス・同じ学科の場合
            boolean isExactMatch = active.getSessionDate().equals(date) 
                                && active.getTimeSlot().getSlotId().equals(slotId)
                                && active.getDepartment().getDepartmentId().equals(departmentId)
                                && active.getClassroom().getClassroomId().equals(classroomId)
                                && active.getSubject().getSubjectId().equals(subjectId);

            // 同じ授業の場合
            if (isExactMatch) {
                return active;
            }
        }

        // 新規セッション作成
        TimeSlotEntity timeSlot = timeSlotRepository.findById(slotId).orElseThrow();
        SubjectEntity subject = subjectRepository.findById(subjectId).orElseThrow();
        ClassroomEntity classroom = classroomRepository.findById(classroomId).orElseThrow();
        DepartmentEntity department = departmentRepository.findById(departmentId).orElseThrow();
        SessionEntity session = new SessionEntity();

        // セッション情報設定
        session.setUserId(userId);
        session.setSessionDate(date);
        session.setStartTime(LocalDateTime.now());
        session.setTimeSlot(timeSlot);
        session.setSubject(subject);
        session.setClassroom(classroom);
        session.setDepartment(department); 
        session.setTargetGrade(targetGrade);
        session.setSessionFlag(false); 

        // 時間割情報設定
        timetableRepository.findByUserIdAndDateAndSlotId(userId, date, slotId)
                .ifPresent(session::setTimeTable);

        return sessionRepository.save(session);
    }

    // 出席者一覧取得
    @Transactional(readOnly = true)
    public List<SessionDto> getSessionAttendees(Integer sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        // 履修生取得
        List<EnrollmentsEntity> enrollments = enrollmentsRepository.findByDepartmentIdAndGrade(
            session.getDepartment().getDepartmentId(), 
            session.getTargetGrade()
        );

        // ログ取得
        List<EntryLogEntity> logs = entryLogService.getLogsForEndSession(session, now);

        // 直前授業の出席状況マップ取得
        Map<Integer, Integer> prevStatusMap = getPreviousSessionStatusMap(session);

        // 判定基準時間
        List<SessionDto> result = new ArrayList<>();
        LocalDateTime baseTime = session.getStartTime();
        LocalDateTime lateBoundary = baseTime.plusMinutes(20);
        LocalDateTime absentBoundary = baseTime.plusMinutes(30);

        // 各履修生についてDTO作成
        for (EnrollmentsEntity enrollment : enrollments) {

            // ログ確認
            Integer studentId = enrollment.getStudent().getUserId();
            EntryLogEntity myLog = logs.stream()
                .filter(l -> l.getUserId().equals(studentId))
                .findFirst().orElse(null);

            // DTO作成
            SessionDto dto = sessionDtoMapper.toDto(session, enrollment, myLog, null);
            if (myLog != null) {
                if (myLog.getEntryTime() != null) {
                    if (myLog.getEntryTime().isAfter(absentBoundary)) {
                        dto.setStatusId(2); // 欠席
                    } else if (myLog.getEntryTime().isAfter(lateBoundary)) {
                        dto.setStatusId(3); // 遅刻
                    } else {
                        dto.setStatusId(1); // 出席
                    }
                } else {
                    dto.setStatusId(null); 
                }
            } else {

                // 連続授業判定
                if (prevStatusMap.containsKey(studentId)) {
                    Integer prevStatus = prevStatusMap.get(studentId);

                    // 直前授業のステータスを引き継ぐ
                    if (prevStatus == 2) {
                        dto.setStatusId(2);
                        dto.setEntryTime("--:-- --");
                    } else if (prevStatus == 6) {
                        dto.setStatusId(6);
                        dto.setEntryTime("--:-- --");
                    } else {
                        dto.setStatusId(1); 
                        dto.setEntryTime("(連続)");
                    }
                } else {
                    dto.setStatusId(2); 
                    dto.setEntryTime("--:-- --");
                }
            }
            result.add(dto);
        }
        return result;
    }

    // 直前授業の出席状況マップを取得するメソッド
    private Map<Integer, Integer> getPreviousSessionStatusMap(SessionEntity currentSession) {
        Map<Integer, Integer> map = new HashMap<>();

        // 同じ日付・学年・科目・学科のセッションを取得
        List<SessionEntity> sameDaySessions = sessionRepository.findByDepartmentAndTargetGradeAndSubjectAndSessionDate(
                currentSession.getDepartment(),
                currentSession.getTargetGrade(),
                currentSession.getSubject(),
                currentSession.getSessionDate()
        );
        
        // 直前の時限を探す
        Optional<SessionEntity> prevSessionOpt = sameDaySessions.stream()
                .filter(s -> s.getTimeSlot().getSlotId() < currentSession.getTimeSlot().getSlotId())
                .max(Comparator.comparingInt(s -> s.getTimeSlot().getSlotId()));

        // 教室が同じ場合のみ出席状況を取得
        if (prevSessionOpt.isPresent()) {
            SessionEntity prevSession = prevSessionOpt.get();

            // 教室の一致チェック
            boolean isSameClassroom = prevSession.getClassroom().getClassroomId()
                                            .equals(currentSession.getClassroom().getClassroomId());

            // 同じ教室の場合、出席状況をマップに格納
            if (isSameClassroom) {
                List<AttendanceEntity> prevAttendances = attendanceRepository.findBySessionId(prevSession.getSessionId());
                for (AttendanceEntity att : prevAttendances) {
                    map.put(att.getStudent().getUserId(), att.getStatus().getStatusId());
                }
            }
        }
        return map;
    }

    // 授業終了処理
    @Transactional
    public void endSession(Integer sessionId, Map<Integer, Integer> manualChanges) {
        SessionEntity session = sessionRepository.findById(sessionId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        // 入室ログ取得
        List<EntryLogEntity> logsToProcess = entryLogService.getLogsForEndSession(session, now);
        sessionAttendanceService.registerAttendance(session, logsToProcess, manualChanges);
        entryLogService.markLogsAsProcessed(logsToProcess, now);
        session.setSessionFlag(true); 
        session.setEndTime(now);
        sessionRepository.save(session);
    }

    // 授業強制終了処理
    @Transactional
    public void forceEndSession(Integer sessionId) {

        // セッション取得
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.setEndTime(LocalDateTime.now());
        session.setSessionFlag(true); 
        sessionRepository.save(session);
    }

    // 授業キャンセル処理
    @Transactional
    public void cancelSession(Integer sessionId) {
        attendanceRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    // セッション取得
    public SessionEntity getSession(Integer sessionId) {
        return sessionRepository.findById(sessionId).orElseThrow();
    }

    // ユーザーの実施中セッション取得
    public List<SessionEntity> findActiveSessionsByUserId(Integer userId) {
        return sessionRepository.findActiveSessionsByUserId(userId);
    }

    // ユーザーの実施中セッション取得（単一）
    public Optional<SessionEntity> findActiveSessionByUserId(Integer userId) {
        List<SessionEntity> sessions = findActiveSessionsByUserId(userId);

        // 実施中セッションがない場合は空を返す
        if (sessions.isEmpty()) {
            return Optional.empty();
        }
        // 単一のセッションを返す
        return Optional.of(sessions.get(0));
    }
}