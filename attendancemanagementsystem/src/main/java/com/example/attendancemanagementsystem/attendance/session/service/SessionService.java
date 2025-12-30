package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.session.dto.SessionDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.AttendanceStatusEntity;
import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository;
import com.example.attendancemanagementsystem.common.repository.EntryLogRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final EntryLogRepository entryLogRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository; 
    private final AttendanceStatusRepository attendanceStatusRepository; 

    public SessionService(SessionRepository sessionRepository,
                          EntryLogRepository entryLogRepository,
                          AttendanceRepository attendanceRepository,
                          StudentRepository studentRepository,
                          AttendanceStatusRepository attendanceStatusRepository) {
        this.sessionRepository = sessionRepository;
        this.entryLogRepository = entryLogRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
    }

    /**
     * 画面表示用
     */
    public List<SessionDto> getSessionAttendees(Integer sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // 現在の「年度」を計算 (4月始まりの年度)
        int currentYear = LocalDate.now().getYear();
        if (LocalDate.now().getMonthValue() < 4) currentYear--; 
        
        Integer targetGrade = session.getTargetGrade();
        if (targetGrade == null) targetGrade = 1; 

        // ★修正: リポジトリの新しいメソッドを使用
        List<StudentEntity> allStudents = studentRepository.findByDepartmentAndGrade(
                session.getTargetDepartmentId(), 
                targetGrade,
                currentYear
        );

        LocalDateTime searchStart = session.getStartTime().minusMinutes(30);
        List<EntryLogEntity> logs = entryLogRepository.findByClassroomIdAndEntryTimeBetween(
                session.getActualClassroomId(), searchStart, LocalDateTime.now()
        );

        List<AttendanceEntity> existingAttendances = attendanceRepository.findBySessionId(sessionId);
        Map<Integer, AttendanceEntity> attendanceMap = existingAttendances.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getUserId(), a -> a));

        List<SessionDto> result = new ArrayList<>();
        int lateLimitMinutes = 20; 
        LocalDateTime lateBoundary = session.getStartTime().plusMinutes(lateLimitMinutes);

        for (StudentEntity student : allStudents) {
            SessionDto dto = new SessionDto();
            Integer userId = student.getUserId();

            dto.setUserId(userId);
            
            // StudentEntityの構造に合わせて名前取得 (getUsers()経由)
            String name = (student.getUsers() != null) ? student.getUsers().getName() : "Unknown";
            dto.setStudentName(name);
            
            dto.setGradeClass(targetGrade + "年"); 

            if (attendanceMap.containsKey(userId)) {
                AttendanceEntity saved = attendanceMap.get(userId);
                // StatusEntityからIDを取得
                dto.setStatusId(saved.getStatus().getStatusId());
                dto.setEntryTime("--:--"); 
                logs.stream().filter(l -> l.getUserId().equals(userId)).findFirst()
                    .ifPresent(l -> dto.setEntryTime(l.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm"))));
            } else {
                EntryLogEntity myLog = logs.stream()
                        .filter(l -> l.getUserId().equals(userId)).findFirst().orElse(null);

                if (myLog != null) {
                    dto.setEntryTime(myLog.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                    dto.setStatusId(myLog.getEntryTime().isAfter(lateBoundary) ? 3 : 1);
                } else {
                    dto.setEntryTime("--:--");
                    dto.setStatusId(2); 
                }
            }
            result.add(dto);
        }
        return result;
    }

    /**
     * updateStatus (未使用)
     */
    public void updateStatus(Integer sessionId, Integer userId, Integer newStatusId) {
    }

    /**
     * 授業終了処理 (一括保存)
     */
    @Transactional
    public void endSession(Integer sessionId, Map<Integer, Integer> manualChanges) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        session.setEndTime(LocalDateTime.now());
        session.setSessionStatus(0);
        sessionRepository.save(session);

        List<SessionDto> finalStates = getSessionAttendees(sessionId);
        
        for (SessionDto dto : finalStates) {
            AttendanceEntity attendance = attendanceRepository.findBySessionIdAndStudent_UserId(sessionId, dto.getUserId())
                    .orElse(new AttendanceEntity());

            if (attendance.getAttendanceId() == null) {
                attendance.setSessionId(sessionId);
                StudentEntity student = studentRepository.findById(dto.getUserId()).orElse(null);
                if (student == null) continue;
                attendance.setStudent(student);
                attendance.setCreatedAt(LocalDateTime.now());
            }

            // 確定ロジック
            Integer tempStatusId = dto.getStatusId();
            
            if (manualChanges != null && manualChanges.containsKey(dto.getUserId())) {
                tempStatusId = manualChanges.get(dto.getUserId());
            }

            // ラムダ式内で使うためにfinalな変数にする
            Integer statusIdToFind = tempStatusId;

            AttendanceStatusEntity status = attendanceStatusRepository.findById(statusIdToFind)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Status ID: " + statusIdToFind));
            
            attendance.setStatusId(status);

            attendanceRepository.save(attendance);
        }
    }
}