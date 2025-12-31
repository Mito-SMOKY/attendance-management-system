package com.example.attendancemanagementsystem.attendance.session.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // 現在時刻から自動入力対象の時間割データを特定する
    public Optional<TimetableEntity> findCurrentScheduledLesson(Integer teacherUserId) {

        // 現在の日時を取得
        LocalDateTime now = LocalDateTime.now();

        // 今日の日付を取得
        LocalDate today = now.toLocalDate();
        
        // 講師IDと日付に基づき今日の時間割を取得
        List<TimetableEntity> todaysLessons = timetableRepository.findByUserIdAndDate(teacherUserId, today);

        // 現在時刻が授業枠に収まっているものを抽出する
        return todaysLessons.stream().filter(lesson -> {
            return true; 
        }).findFirst();
    }

    // 出席状況の取得
    public List<SessionDto> getSessionAttendees(Integer sessionId) {

        // セッションを取得し存在しない場合はエラーを投げる
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // 年度を計算する（4月始まりに対応）
        int currentYear = LocalDate.now().getYear();
        if (LocalDate.now().getMonthValue() < 4) currentYear--;

        // 対象学年を取得
        Integer targetGrade = session.getTargetGrade() != null ? session.getTargetGrade() : 1;

        // 対象学科・学年の全学生を取得
        List<StudentEntity> allStudents = studentRepository.findByDepartmentAndGrade(
                session.getTargetDepartmentId(), targetGrade, currentYear
        );

        // 授業開始15分前から現在までの入室ログを検索
        LocalDateTime searchStart = session.getStartTime().minusMinutes(15);
        List<EntryLogEntity> logs = entryLogRepository.findByClassroomIdAndEntryTimeBetween(
                session.getActualClassroomId(), searchStart, LocalDateTime.now()
        );

        // 既存の保存済み出席データを取得
        List<AttendanceEntity> existingAttendances = attendanceRepository.findBySessionId(sessionId);

        // 学生IDをキーとしたマップに変換
        Map<Integer, AttendanceEntity> attendanceMap = existingAttendances.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getUserId(), a -> a));

        List<SessionDto> result = new ArrayList<>();

        // 遅刻の境界時刻（開始20分後）を設定
        LocalDateTime lateBoundary = session.getStartTime().plusMinutes(20);

        for (StudentEntity student : allStudents) {
            SessionDto dto = new SessionDto();
            Integer userId = student.getUserId();
            dto.setUserId(userId);

            // 学生名を設定
            String name = (student.getUsers() != null) ? student.getUsers().getName() : "Unknown";
            dto.setStudentName(name);
            dto.setGradeClass(targetGrade + "年");

            // 既に保存済みのデータがある場合
            if (attendanceMap.containsKey(userId)) {
                AttendanceEntity saved = attendanceMap.get(userId);
                dto.setStatusId(saved.getStatus().getStatusId());
                dto.setEntryTime("--:--");

                // 最新の入室時刻をログから抽出
                logs.stream().filter(l -> l.getUserId().equals(userId)).findFirst()
                    .ifPresent(l -> dto.setEntryTime(l.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm"))));
            } 

            // 保存されていない場合はリアルタイムで判定
            else {
                EntryLogEntity myLog = logs.stream()
                        .filter(l -> l.getUserId().equals(userId)).findFirst().orElse(null);

                if (myLog != null) {

                    // 入室時刻を設定
                    dto.setEntryTime(myLog.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm")));

                    // 欠席の境界時刻（開始30分後）を設定
                    LocalDateTime absentBoundary = session.getStartTime().plusMinutes(30);

                    // 入室時刻に基づき欠席・遅刻・出席を判定
                    if (myLog.getEntryTime().isAfter(absentBoundary)) {
                        dto.setStatusId(2); 
                    } else if (myLog.getEntryTime().isAfter(lateBoundary)) {
                        dto.setStatusId(3); 
                    } else {
                        dto.setStatusId(1); 
                    }
                } else {

                    // 前の授業からの連続受講をチェック
                    Optional<AttendanceEntity> prev = attendanceRepository.findPreviousAttendanceInSameRoom(
                            userId, session.getActualClassroomId(),
                            session.getStartTime().minusMinutes(30), session.getStartTime()
                    );

                    if (prev.isPresent()) {
                        dto.setEntryTime("(連続)"); 
                        dto.setStatusId(1);
                    } else {
                        dto.setEntryTime("--:--");
                        dto.setStatusId(2);
                    }
                }
            }
            result.add(dto);
        }
        return result;
    }

    // 授業終了処理
    @Transactional
    public void endSession(Integer sessionId, Map<Integer, Integer> manualChanges) {

        // セッションを取得
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // セッションの終了時刻と状態を更新して保存
        LocalDateTime now = LocalDateTime.now();
        session.setEndTime(now);
        session.setSessionStatus(0);
        sessionRepository.save(session);

        // 最終的な出席状況を取得
        List<SessionDto> finalStates = getSessionAttendees(sessionId);

        for (SessionDto dto : finalStates) {

            // 出席レコードを取得または新規作成
            AttendanceEntity attendance = attendanceRepository.findBySessionIdAndStudent_UserId(sessionId, dto.getUserId())
                    .orElse(new AttendanceEntity());

            // 新規レコードの場合は初期設定を行う
            if (attendance.getAttendanceId() == null) {
                attendance.setSessionId(sessionId);

                // セッションに紐づく時間割エンティティをセット
                attendance.setTimeTable(session.getTimetable());
                StudentEntity student = studentRepository.findById(dto.getUserId()).orElse(null);
                if (student == null) continue;
                attendance.setStudent(student);
                attendance.setCreatedAt(now);
            }

            // 手動変更があれば優先してステータスを決定
            Integer finalStatusId = dto.getStatusId();
            if (manualChanges != null && manualChanges.containsKey(dto.getUserId())) {
                finalStatusId = manualChanges.get(dto.getUserId());
            }

            // ステータスをセットして保存
            AttendanceStatusEntity status = attendanceStatusRepository.findById(finalStatusId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Status ID"));
            attendance.setStatusId(status);

            attendanceRepository.save(attendance);
        }
        
        // 授業に関連する未処理ログのクリーンアップを実行
        processRemainingLogs(session, now);
    }

    // 授業時間内の入室ログを処理済みにマークする
    private void processRemainingLogs(SessionEntity session, LocalDateTime now) {

        // 判定リミット時刻を計算
        LocalDateTime searchLimit = session.getStartTime().plusMinutes(85);
        
        // リミットと現在の早い方を検索終了時刻にする
        LocalDateTime searchEnd = now.isBefore(searchLimit) ? now : searchLimit;

        // 授業開始15分前からリミットまでのログを検索
        List<EntryLogEntity> logs = entryLogRepository.findByClassroomIdAndEntryTimeBetween(
            session.getActualClassroomId(),
            session.getStartTime().minusMinutes(15),
            searchEnd
        );

        // 未処理のログをすべて処理済みに更新
        for (EntryLogEntity log : logs) {
            if (log.getIsProcessed() == null || log.getIsProcessed() == 0) {
                log.setIsProcessed(1);
                log.setProcessedAt(now);
                entryLogRepository.save(log);
            }
        }
    }
}