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
     * 変更点:
     * 1. 「違う時限」の授業が進行中の場合のみエラーにする
     * 2. 「同じ時限」であれば複数クラスの作成を許可する
     */
    @Transactional
    public SessionEntity startSession(Integer userId, Integer subjectId, LocalDate date, Integer slotId, 
                                      Integer classroomId, Integer departmentId, Integer targetGrade) {
        
        // 1. このユーザーの「現在進行中(Active)」の全セッションを取得
        List<SessionEntity> activeSessions = sessionRepository.findActiveSessionsByUserId(userId);

        for (SessionEntity active : activeSessions) {
            
            // A. 「違う時限」の授業が進行中かチェック (ここが重要)
            if (!active.getTimeSlot().getSlotId().equals(slotId)) {
                throw new IllegalStateException("終了していない他時限(" + active.getTimeSlot().getSlotId() + "限)の授業があります。先に終了させてください。");
            }

            // B. 「全く同じ授業（再開）」かチェック
            boolean isExactMatch = active.getSessionDate().equals(date) 
                                && active.getTimeSlot().getSlotId().equals(slotId)
                                && active.getDepartment().getDepartmentId().equals(departmentId)
                                && active.getClassroom().getClassroomId().equals(classroomId)
                                && active.getSubject().getSubjectId().equals(subjectId);

            if (isExactMatch) {
                // 条件が完全一致するなら、既存のセッションを返して「再開」扱いにする
                return active;
            }
            
            // C. 「同じ時限」だが「違う授業(クラスや科目が違う)」の場合
            // ループを継続（何もしない）。
            // エラーを投げずにループを抜ければ、下の新規作成処理に進むため、2つ目の授業が作られる。
        }
        
        // --- 以下、新規作成ロジック (提供コードのまま) ---

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
        session.setSessionFlag(false); // 授業中フラグ

        timetableRepository.findByUserIdAndDateAndSlotId(userId, date, slotId)
                .ifPresent(session::setTimeTable);

        return sessionRepository.save(session);
    }

    /**
     * 出席者リスト表示 (変更なし)
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

        // 3. 直前の授業（同教室）の出席状況を取得
        Map<Integer, Integer> prevStatusMap = getPreviousSessionStatusMap(session);

        // 4. 判定ロジック
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
                // 連続授業判定
                if (prevStatusMap.containsKey(studentId)) {
                    Integer prevStatus = prevStatusMap.get(studentId);
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

    /**
     * Helper: 直前の授業のStatusMapを取得 (変更なし)
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

            // 教室の一致チェック
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
     * 授業終了処理 (変更なし)
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

    /**
     * ★追加: 強制終了処理
     * ポップアップから呼び出し、出席判定を行わずに授業を「終了済み」にする
     */
    @Transactional
    public void forceEndSession(Integer sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        session.setEndTime(LocalDateTime.now());
        session.setSessionFlag(true); // 実施中(false) -> 終了(true)
        
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

    public List<SessionEntity> findActiveSessionsByUserId(Integer userId) {
        return sessionRepository.findActiveSessionsByUserId(userId);
    }

    public Optional<SessionEntity> findActiveSessionByUserId(Integer userId) {
        List<SessionEntity> sessions = findActiveSessionsByUserId(userId);
        if (sessions.isEmpty()) {
            return Optional.empty();
        }
        // 複数ある場合は、とりあえず先頭のものを返す
        return Optional.of(sessions.get(0));
    }
}