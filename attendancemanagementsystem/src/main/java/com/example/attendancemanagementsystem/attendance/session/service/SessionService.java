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
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.AttendanceStatusEntity;
import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository;
import com.example.attendancemanagementsystem.common.repository.EntryLogRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;
    private final EntryLogRepository entryLogRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final AttendanceStatusRepository attendanceStatusRepository;
    private final TimetableRepository timetableRepository;

    // コンストラクタ
    public SessionService(SessionRepository sessionRepository,
                        EntryLogRepository entryLogRepository,
                        AttendanceRepository attendanceRepository,
                        StudentRepository studentRepository,
                        AttendanceStatusRepository attendanceStatusRepository,
                        TimetableRepository timetableRepository) {
        this.sessionRepository = sessionRepository;
        this.entryLogRepository = entryLogRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
        this.timetableRepository = timetableRepository;
    }

    // 画面で指定された「日付」と「科目」から時間割を特定するメソッド
        public Optional<TimetableEntity> findTimetableBySchedule(Integer teacherUserId, LocalDate date, Integer subjectId) {
        
        // その日の講師の授業を全て取得
        List<TimetableEntity> todaysLessons = timetableRepository.findByUserIdAndDate(teacherUserId, date);
        
        // 科目IDが一致するものを探して返す
        return todaysLessons.stream()
                .filter(lesson -> lesson.getSubjectId() != null && lesson.getSubjectId().equals(subjectId))
                .findFirst();
    }

    // 現在時刻から時間割データを特定する
    public Optional<TimetableEntity> findCurrentScheduledLesson(Integer teacherUserId) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        LocalDate today = now.toLocalDate();
        List<TimetableEntity> todaysLessons = timetableRepository.findByUserIdAndDate(teacherUserId, today);
        
        // 現在時刻が「授業開始15分前 ～ 授業終了」の範囲に含まれるものを探す
        return todaysLessons.stream().filter(lesson -> {
            TimeSlotEntity slot = lesson.getTimeSlot();
            if (slot == null) return false;
            LocalTime start = slot.getStartTime();
            LocalTime end = slot.getEndTime();
            if (start == null || end == null) return false;
            return currentTime.isAfter(start.minusMinutes(15)) && currentTime.isBefore(end);
        }).findFirst();
    }

    // 出席状況を取得する
    public List<SessionDto> getSessionAttendees(Integer sessionId) {

        // セッション情報を取得
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        // 学年を計算（4月より前なら前年度として扱う）
        int currentYear = LocalDate.now().getYear();
        if (LocalDate.now().getMonthValue() < 4) currentYear--;
        Integer targetGrade = session.getTargetGrade() != null ? session.getTargetGrade() : 1;
        
        // 対象となる学科・学年の生徒全員を取得
        List<StudentEntity> allStudents = studentRepository.findByDepartmentAndGrade(
                session.getTargetDepartmentId(), targetGrade, currentYear
        );
        
        // タッチログの検索範囲を設定（授業開始15分前から現在まで）
        LocalDateTime searchStart = session.getStartTime().minusMinutes(15);
        List<EntryLogEntity> logs = entryLogRepository.findByClassroomIdAndEntryTimeBetween(
                session.getActualClassroomId(), searchStart, LocalDateTime.now()
        );
        
        // 既にDBに保存されている出席情報があれば取得
        List<AttendanceEntity> existingAttendances = attendanceRepository.findBySessionId(sessionId);
        Map<Integer, AttendanceEntity> attendanceMap = existingAttendances.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getUserId(), a -> a));
        
        List<SessionDto> result = new ArrayList<>();
        
        // 遅刻と判定する境界線（授業開始 + 20分）
        LocalDateTime lateBoundary = session.getStartTime().plusMinutes(20);

        // 生徒一人ひとりについてループ処理
        for (StudentEntity student : allStudents) {
            SessionDto dto = new SessionDto();
            Integer userId = student.getUserId();
            dto.setUserId(userId);
            String name = (student.getUsers() != null) ? student.getUsers().getName() : "Unknown";
            dto.setStudentName(name);
            dto.setGradeClass(targetGrade + "年");
            
            // 既に出席情報が保存されている場合（確定後の再表示など）
            if (attendanceMap.containsKey(userId)) {
                AttendanceEntity saved = attendanceMap.get(userId);
                dto.setStatusId(saved.getStatus().getStatusId());
                dto.setEntryTime("--:--");
                
                // ログがあれば時間を表示
                logs.stream().filter(l -> l.getUserId().equals(userId)).findFirst()
                    .ifPresent(l -> dto.setEntryTime(l.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm"))));
            } 

            // まだ保存されていない場合
            else {

                // この生徒のタッチログを探す
                EntryLogEntity myLog = logs.stream()
                        .filter(l -> l.getUserId().equals(userId)).findFirst().orElse(null);
                
                if (myLog != null) {

                    // ログがある場合：時間によってステータスを自動判定
                    dto.setEntryTime(myLog.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                    
                    LocalDateTime absentBoundary = session.getStartTime().plusMinutes(30);
                    
                    if (myLog.getEntryTime().isAfter(absentBoundary)) { 
                        dto.setStatusId(2); // 30分以上遅刻は「欠席」扱い
                    } else if (myLog.getEntryTime().isAfter(lateBoundary)) { 
                        dto.setStatusId(3); // 20分以上遅刻は「遅刻」
                    } else { 
                        dto.setStatusId(1); // それ以外は「出席」
                    }
                } else {

                    // ログがない場合：連続授業の判定
                    Optional<AttendanceEntity> prev = attendanceRepository.findPreviousAttendanceInSameRoom(
                            userId, session.getActualClassroomId(),
                            session.getStartTime().minusMinutes(30), session.getStartTime()
                    );
                    
                    if (prev.isPresent()) { 
                        dto.setEntryTime("(連続)"); 
                        dto.setStatusId(1); // 連続授業なら出席扱い
                    } else { 
                        dto.setEntryTime("--:--"); 
                        dto.setStatusId(2); // ログも連続記録もなければ「欠席」
                    }
                }
            }
            result.add(dto);
        }
        return result;
    }

    // 授業終了処理メソッド
    @Transactional
    public void endSession(Integer sessionId, Map<Integer, Integer> manualChanges) {

        // セッション情報を取得
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        // 時間割を取得する
        TimetableEntity timeTable = session.getTimeTable();
        LocalDateTime now = LocalDateTime.now();
        session.setEndTime(now);
        session.setSessionStatus(0); 
        sessionRepository.save(session);

        // 最終的な出席状況を取得（手動変更があれば適用するため）
        List<SessionDto> finalStates = getSessionAttendees(sessionId);
        
        for (SessionDto dto : finalStates) {

            // 既存の出席データがあれば取得、なければ新規作成
            AttendanceEntity attendance = attendanceRepository.findBySessionIdAndStudent_UserId(sessionId, dto.getUserId())
                    .orElse(new AttendanceEntity());
            
            if (attendance.getAttendanceId() == null) {
                // 新規作成時の設定
                attendance.setSessionId(sessionId);
                
                //  時間割が存在する場合のみセットする
                if (timeTable != null) {
                    attendance.setTimeTable(timeTable);
                }
                
                StudentEntity student = studentRepository.findById(dto.getUserId()).orElse(null);
                if (student == null) continue; 
                attendance.setStudent(student);
                attendance.setCreatedAt(now);
            }
            
            // ステータスの決定（手動変更があればそれを優先）
            Integer finalStatusId = dto.getStatusId();
            if (manualChanges != null && manualChanges.containsKey(dto.getUserId())) {
                finalStatusId = manualChanges.get(dto.getUserId());
            }
            
            // ステータスIDからエンティティを取得してセット
            AttendanceStatusEntity status = attendanceStatusRepository.findById(finalStatusId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Status ID"));
            attendance.setStatusId(status);
            
            // 保存実行
            attendanceRepository.save(attendance);
        }
        
        // 使用したタッチログを「処理済み」にする
        processRemainingLogs(session, now);
    }

    // ログの後処理メソッド
    private void processRemainingLogs(SessionEntity session, LocalDateTime now) {

        // ログの検索終了時刻を設定
        LocalDateTime searchLimit = session.getStartTime().plusMinutes(85);
        LocalDateTime searchEnd = now.isBefore(searchLimit) ? now : searchLimit;
        
        // 対象範囲のログを取得
        List<EntryLogEntity> logs = entryLogRepository.findByClassroomIdAndEntryTimeBetween(
            session.getActualClassroomId(), session.getStartTime().minusMinutes(15), searchEnd
        );
        
        for (EntryLogEntity log : logs) {

            // まだ未処理のログであれば、処理済み(1)に更新
            if (log.getIsProcessed() == null || log.getIsProcessed() == 0) {
                log.setIsProcessed(1);
                log.setProcessedAt(now);
                entryLogRepository.save(log);
            }
        }
    }

    //セッションのキャンセル処理
    @Transactional
    public void cancelSession(Integer sessionId) {

        // このセッションに紐づく出席データが万が一存在すれば先に削除
        attendanceRepository.deleteBySessionId(sessionId);
        
        // セッション自体を削除
        sessionRepository.deleteById(sessionId);
    }
}