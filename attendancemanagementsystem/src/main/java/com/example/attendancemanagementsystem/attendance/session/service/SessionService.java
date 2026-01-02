package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.session.dto.SessionDto;
import com.example.attendancemanagementsystem.attendance.session.mapper.SessionDtoMapper;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final TimetableRepository timetableRepository;
    private final EnrollmentsRepository enrollmentsRepository;
    private final AttendanceRepository attendanceRepository;
    private final EntryLogService entryLogService;                  // ログ処理担当
    private final SessionAttendanceService sessionAttendanceService; // 出席確定担当
    private final SessionDtoMapper sessionDtoMapper;                // データ変換担当

    // 指定された条件（日付・講師・科目）に合う時間割を探す
    public Optional<TimetableEntity> findTimetableBySchedule(Integer teacherUserId, LocalDate date, Integer subjectId) {
        List<TimetableEntity> todaysLessons = timetableRepository.findByUserIdAndDate(teacherUserId, date);
        return todaysLessons.stream()
                .filter(lesson -> lesson.getSubjectId() != null && lesson.getSubjectId().equals(subjectId))
                .findFirst();
    }

    // 現在時刻に行われている授業を自動判定して探す
    public Optional<TimetableEntity> findCurrentScheduledLesson(Integer teacherUserId) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        List<TimetableEntity> todaysLessons = timetableRepository.findByUserIdAndDate(teacherUserId, now.toLocalDate());
        
        return todaysLessons.stream().filter(lesson -> {
            TimeSlotEntity slot = lesson.getTimeSlot();
            if (slot == null) return false;
            
            // 授業開始15分前から終了時間までを「実施中」とみなす
            return currentTime.isAfter(slot.getStartTime().minusMinutes(15)) 
                && currentTime.isBefore(slot.getEndTime());
        }).findFirst();
    }

    // 授業開始
    @Transactional
    public SessionEntity startSession(Integer teacherUserId, Integer subjectId, LocalDate date, LocalDateTime startDateTime, 
                                    Integer classroomId, Integer targetDepartmentId, Integer targetGrade, String note) {
        
        SessionEntity session = new SessionEntity();
        
        // 時間割検索
        Optional<TimetableEntity> scheduledOpt = findTimetableBySchedule(teacherUserId, date, subjectId);

        if (scheduledOpt.isPresent()) {

            // 時間割有
            TimetableEntity tt = scheduledOpt.get();
            session.setTimeTable(tt);

            // 教室指定がなければ時間割の教室を使う
            session.setActualClassroomId(classroomId == null ? tt.getClassroomId() : classroomId);
        } else {
            // 時間割無
            session.setTimeTable(null);
            session.setActualClassroomId(classroomId);
        }

        // セッション情報の入力
        session.setTargetDepartmentId(targetDepartmentId);
        session.setTargetGrade(targetGrade);
        session.setSessionDate(date);
        session.setStartTime(startDateTime);
        session.setNote(note); 
        session.setSessionStatus(1); // 1:実施中

        // 保存
        return sessionRepository.save(session);
    }

    // 授業終了
    @Transactional
    public void endSession(Integer sessionId, Map<Integer, Integer> manualChanges) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        LocalDateTime now = LocalDateTime.now();

        // 出席情報の確定処理
        sessionAttendanceService.finalizeAttendance(session, manualChanges, now);

        // ログの事後処理
        entryLogService.processRemainingLogs(session, now);

        // セッション自体の終了ステータス更新
        session.setEndTime(now);
        session.setSessionStatus(0); 
        sessionRepository.save(session);
    }

    // 出席状況リストを作成して返す
    public List<SessionDto> getSessionAttendees(Integer sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        Integer targetGrade = session.getTargetGrade() != null ? session.getTargetGrade() : 1;
        
        // 生徒名簿を取得
        List<EnrollmentsEntity> enrollments = enrollmentsRepository.findByDepartmentIdAndGrade(
                session.getTargetDepartmentId(), targetGrade);
        
        // ログ情報を取得
        List<EntryLogEntity> logs = entryLogService.findLogsForSession(
                session.getActualClassroomId(), session.getStartTime().minusMinutes(15), LocalDateTime.now());
        
        // 既に保存済みの出席情報があれば取得（Map化して検索しやすくする）
        List<AttendanceEntity> existingAttendances = attendanceRepository.findBySessionId(sessionId);
        Map<Integer, AttendanceEntity> attendanceMap = existingAttendances.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getUserId(), a -> a));

        List<SessionDto> result = new ArrayList<>();
        LocalDateTime lateBoundary = session.getStartTime().plusMinutes(20);   // 遅刻の境界線
        LocalDateTime absentBoundary = session.getStartTime().plusMinutes(30); // 欠席の境界線

        // 生徒一人ひとりについてループ処理
        for (EnrollmentsEntity enrollment : enrollments) {
            Integer userId = enrollment.getStudent().getUserId();
            EntryLogEntity myLog = logs.stream().filter(l -> l.getUserId().equals(userId)).findFirst().orElse(null);
            AttendanceEntity myAtt = attendanceMap.get(userId);

            // Mapperを使ってDTO（表示用データ）の土台を作成
            SessionDto dto = sessionDtoMapper.toDto(session, enrollment, myLog, myAtt);

            // ステータスの詳細判定
            if (attendanceMap.containsKey(userId)) {

                // 【確定済みの場合】時間は非表示（または固定）
                dto.setEntryTime("--:--");
                if (myLog != null) {
                    dto.setEntryTime(myLog.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                }
            } else {
                // 【未確定（授業中）の場合】ログの時間を見て遅刻・欠席をリアルタイム判定
                if (myLog != null) {
                    dto.setEntryTime(myLog.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                    
                    if (myLog.getEntryTime().isAfter(absentBoundary)) {
                        dto.setStatusId(2); // 30分以上遅れ -> 欠席
                    } else if (myLog.getEntryTime().isAfter(lateBoundary)) {
                        dto.setStatusId(3); // 20分以上遅れ -> 遅刻
                    } else {
                        dto.setStatusId(1); // それ以外 -> 出席
                    }
                } else {

                    // ログがない場合 -> 連続授業の可能性を確認
                    Optional<AttendanceEntity> prev = attendanceRepository.findContinuousAttendance(
                            userId, session.getActualClassroomId(),
                            session.getStartTime().minusMinutes(30), session.getStartTime());
                    
                    if (prev.isPresent()) {
                        dto.setEntryTime("(連続)");
                        dto.setStatusId(1); // 連続なら出席扱い
                    } else {
                        dto.setEntryTime("--:--");
                        dto.setStatusId(2); // ログも連続もなければ欠席扱い
                    }
                }
            }
            result.add(dto);
        }
        return result;
    }

    // キャンセル処理
    @Transactional
    public void cancelSession(Integer sessionId) {
        attendanceRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }
}