package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.dto.AttendanceMetricsDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.AttendanceStatusEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.common.service.AttendanceCalculationService;
import com.example.attendancemanagementsystem.user.notification.service.NotificationMessageService;

@Service
public class SessionAttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(SessionAttendanceService.class);

    private final EnrollmentsRepository enrollmentsRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceStatusRepository attendanceStatusRepository;
    private final SessionRepository sessionRepository;
    private final UsersRepository usersRepository;
    private final AttendanceCalculationService attendanceCalculationService;
    private final NotificationMessageService notificationMessageService;

    public SessionAttendanceService(
            EnrollmentsRepository enrollmentsRepository,
            AttendanceRepository attendanceRepository,
            AttendanceStatusRepository attendanceStatusRepository,
            SessionRepository sessionRepository,
            UsersRepository usersRepository,
            AttendanceCalculationService attendanceCalculationService,
            NotificationMessageService notificationMessageService) {
        this.enrollmentsRepository = enrollmentsRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
        this.sessionRepository = sessionRepository;
        this.usersRepository = usersRepository;
        this.attendanceCalculationService = attendanceCalculationService;
        this.notificationMessageService = notificationMessageService;
    }

    // 出席情報の登録
    @Transactional
    public void registerAttendance(SessionEntity session, List<EntryLogEntity> logs, Map<Integer, Integer> manualChanges) {
        
        // ステータスMap取得
        Map<Integer, AttendanceStatusEntity> statusMap = attendanceStatusRepository.findAll().stream()
                .collect(Collectors.toMap(AttendanceStatusEntity::getStatusId, Function.identity()));
        
        // 履修生取得
        List<EnrollmentsEntity> enrollments = enrollmentsRepository.findByDepartmentIdAndGrade(
            session.getDepartment().getDepartmentId(), 
            session.getTargetGrade()
        );

        LocalDateTime baseTime = session.getStartTime();
        LocalDateTime lateBoundary = baseTime.plusMinutes(20);   
        LocalDateTime absentBoundary = baseTime.plusMinutes(30); 

        Map<Integer, Integer> prevStatusMap = getPreviousSessionStatusMap(session);
        List<AttendanceEntity> attendancesToSave = new ArrayList<>();

        for (EnrollmentsEntity enrollment : enrollments) {
            Integer studentId = enrollment.getStudent().getUserId();
            AttendanceEntity attendance = new AttendanceEntity();
            attendance.setSessionId(session.getSessionId());
            attendance.setStudent(enrollment.getStudent());

            Integer statusId;

            if (manualChanges != null && manualChanges.containsKey(studentId)) {
                statusId = manualChanges.get(studentId);
            } else {
                EntryLogEntity myLog = logs.stream()
                    .filter(l -> l.getUserId().equals(studentId))
                    .findFirst().orElse(null);

                if (myLog != null) {
                    if (myLog.getStatusId() != null) {
                        statusId = myLog.getStatusId();
                    } else {
                        if (myLog.getEntryTime() != null) {
                            if (myLog.getEntryTime().isAfter(absentBoundary)) {
                                statusId = 2;
                            } else if (myLog.getEntryTime().isAfter(lateBoundary)) {
                                statusId = 3;
                            } else {
                                statusId = 1;
                            }
                        } else {
                            statusId = 2;
                        }
                    }
                } else {
                    if (prevStatusMap.containsKey(studentId)) {
                        Integer prevStatus = prevStatusMap.get(studentId);
                        if (prevStatus == 2) statusId = 2;
                        else if (prevStatus == 6) statusId = 6;
                        else statusId = 1;
                    } else {
                        statusId = 2;
                    }
                }
            }

            AttendanceStatusEntity statusEntity = statusMap.get(statusId);
            if (statusEntity == null) statusEntity = statusMap.get(2); 
            
            attendance.setStatusId(statusEntity);
            attendancesToSave.add(attendance);
        }

        // 保存実行
        List<AttendanceEntity> savedAttendances = attendanceRepository.saveAll(attendancesToSave);

        // ==========================================
        // ★修正: 安全な欠席判定 & 通知処理
        // ==========================================
        try {
            // セッションから科目情報を取得（nullチェック付き）
            if (session.getSubject() == null) {
                logger.warn("SessionID: {} に紐づくSubjectがnullのため通知をスキップします", session.getSessionId());
                return;
            }
            Integer subjectId = session.getSubject().getSubjectId();
            String subjectName = session.getSubject().getSubjectName();

            for (AttendanceEntity att : savedAttendances) {
                // ステータスチェック (Null安全に比較: IDが2=欠席)
                if (att.getStatus() != null 
                        && Integer.valueOf(2).equals(att.getStatus().getStatusId())) {
                    
                    try {
                        Integer userId = att.getStudent().getUserId();
                        
                        // 通知先ユーザー取得
                        UsersEntity studentUser = usersRepository.findById(userId).orElse(null);
                        
                        if (studentUser != null) {
                            // 計算サービス呼び出し
                            AttendanceMetricsDto metrics = attendanceCalculationService.calculateAttendanceMetrics(userId, subjectId);
                            // 通知作成
                            notificationMessageService.createAbsenceNotification(studentUser, subjectName, metrics);
                        }
                    } catch (Exception e) {
                        // 個別の通知失敗はログに出して継続
                        logger.error("欠席通知作成エラー (UserID: " + att.getStudent().getUserId() + ")", e);
                    }
                }
            }
        } catch (Exception e) {
            // 通知処理全体のエラーもキャッチして、授業終了処理自体は止めない
            logger.error("通知処理全体でエラーが発生しました", e);
        }
        // ==========================================
    }

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
}