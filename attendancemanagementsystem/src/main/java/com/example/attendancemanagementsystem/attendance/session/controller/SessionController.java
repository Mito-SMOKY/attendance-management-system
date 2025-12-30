package com.example.attendancemanagementsystem.attendance.session.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.attendance.session.service.SessionService;
import com.example.attendancemanagementsystem.common.entity.ClassroomEntity;
import com.example.attendancemanagementsystem.common.entity.EntryLogEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.EntryLogRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Controller
@RequestMapping("/session")
public class SessionController {

    private final SessionService sessionService;
    private final ClassroomRepository classroomRepository;
    private final SessionRepository sessionRepository;
    private final EntryLogRepository entryLogRepository;
    private final UsersRepository usersRepository;
    // 追加: 教科情報を取得するため
    private final SubjectRepository subjectRepository;

    public SessionController(SessionService sessionService,
                            ClassroomRepository classroomRepository,
                            SessionRepository sessionRepository,
                            EntryLogRepository entryLogRepository,
                            UsersRepository usersRepository,
                            SubjectRepository subjectRepository) {
        this.sessionService = sessionService;
        this.classroomRepository = classroomRepository;
        this.sessionRepository = sessionRepository;
        this.entryLogRepository = entryLogRepository;
        this.usersRepository = usersRepository;
        this.subjectRepository = subjectRepository;
    }

    /**
     * 1. 授業開始設定画面を表示 (GET)
     */
    @GetMapping
    public String showSetupPage(Model model) {
        // 1. 教室リスト取得
        List<ClassroomEntity> classrooms = classroomRepository.findAll();
        
        // 2. 教科リスト取得 (DBから)
        List<SubjectEntity> subjects = subjectRepository.findAll();

        // 3. コースリスト (仮実装: DBにCourseEntityがある場合はRepositoryから取得してください)
        List<String> courses = List.of("情報工学科 1年", "情報工学科 2年", "情報工学科 3年", "AIシステム科");

        // 4. 初期値の設定
        model.addAttribute("classroomList", classrooms);
        model.addAttribute("subjectList", subjects);
        model.addAttribute("courseList", courses);
        
        // 日付: 本日
        model.addAttribute("defaultDate", LocalDate.now());
        
        // 時間: 現在時刻 (本来はTimeTableRepositoryから現在の時限を取得してセットするとベスト)
        model.addAttribute("defaultTime", LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES));

        return "session/session"; 
    }

    /**
     * 2. セッション開始処理 (API / POST)
     */
    @PostMapping("/start")
    @ResponseBody
    public Map<String, Object> startSession(@RequestBody Map<String, Object> request) {
        // 画面からの入力を取得
        String subjectIdStr = (String) request.get("subjectId"); // IDで来るか名前にするかによる
        String courseName = (String) request.get("courseName");
        String dateStr = (String) request.get("date");
        String timeStr = (String) request.get("time");
        Integer classroomId = Integer.parseInt(request.get("classroomId").toString());

        // Subject情報の取得 (IDで検索して名前を取得、あるいは画面から名前を送る)
        SubjectEntity subject = subjectRepository.findById(Integer.parseInt(subjectIdStr)).orElse(null);
        String subjectName = (subject != null) ? subject.getSubjectName() : "不明な教科";

        // 日時の結合
        LocalDate date = LocalDate.parse(dateStr);
        LocalTime time = LocalTime.parse(timeStr);
        LocalDateTime startDateTime = LocalDateTime.of(date, time);

        // セッション保存
        SessionEntity session = new SessionEntity();
        session.setSessionDate(date);
        session.setStartTime(startDateTime);
        session.setActualClassroomId(classroomId);
        
        // Noteカラムに「コース名 - 教科名」の形式で保存して識別できるようにする
        // (SessionEntityに専用カラムがないため)
        session.setNote(courseName + " : " + subjectName);
        
        session.setSessionStatus(1);

        SessionEntity savedSession = sessionRepository.save(session);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", savedSession.getSessionId());
        return response;
    }

    // ... (showActiveSession, endSession, getAttendees メソッドは変更なしのため省略。以前のまま維持してください) ...
    // 必要であれば前の回答のコードを含めますが、長くなるので省略しています。
    // showActiveSession, endSession, getAttendees はそのまま残してください。
    
    // --- 省略されたメソッドの再掲が必要なら指示してください ---

    /**
     * 3. 実施中画面を表示 (GET) - 変更なし
     */
    @GetMapping("/active/{sessionId}")
    public String showActiveSession(@PathVariable Integer sessionId, Model model) {
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid session Id:" + sessionId));

        ClassroomEntity classroom = classroomRepository.findById(session.getActualClassroomId())
                .orElse(null);
        String classroomName = (classroom != null) ? classroom.getClassroomName() : "不明な教室";

        Map<String, Object> sessionDto = new HashMap<>();
        sessionDto.put("sessionId", session.getSessionId());
        sessionDto.put("className", session.getNote());
        sessionDto.put("startTime", session.getStartTime());
        sessionDto.put("classroomName", classroomName);

        model.addAttribute("sessionDto", sessionDto);

        return "session/sessionActive";
    }

    @PostMapping("/end")
    @ResponseBody
    public Map<String, String> endSession(@RequestBody Map<String, Integer> request) {
        Integer sessionId = request.get("sessionId");
        sessionService.endSession(sessionId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session ended and attendance calculated.");
        return response;
    }

    @GetMapping("/api/attendees/{sessionId}")
    @ResponseBody
    public List<Map<String, Object>> getAttendees(@PathVariable Integer sessionId) {
        SessionEntity session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return List.of();

        LocalDateTime start = session.getStartTime().minusMinutes(30);
        LocalDateTime now = LocalDateTime.now();

        List<EntryLogEntity> logs = entryLogRepository.findByClassroomIdAndEntryTimeBetween(
            session.getActualClassroomId(), start, now
        );

        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        for (EntryLogEntity log : logs) {
            UsersEntity user = usersRepository.findById(log.getUserId()).orElse(null);
            Map<String, Object> map = new HashMap<>();
            map.put("studentId", log.getUserId());
            map.put("name", (user != null) ? user.getName() : "Unknown");
            map.put("entryTime", log.getEntryTime().format(timeFormatter));
            map.put("status", "入室済み");
            result.add(map);
        }
        Collections.reverse(result);
        return result;
    }
}