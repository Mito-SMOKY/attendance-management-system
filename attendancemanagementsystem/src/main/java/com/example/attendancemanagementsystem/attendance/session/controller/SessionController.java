package com.example.attendancemanagementsystem.attendance.session.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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

import com.example.attendancemanagementsystem.attendance.session.dto.SessionDto;
import com.example.attendancemanagementsystem.attendance.session.service.SessionService;
import com.example.attendancemanagementsystem.common.entity.ClassroomEntity;
import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;

@Controller
@RequestMapping("/session")
public class SessionController {

    private final SessionService sessionService;
    private final ClassroomRepository classroomRepository;
    private final SessionRepository sessionRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceStatusRepository attendanceStatusRepository;
    private final DepartmentSubjectRepository departmentSubjectRepository;

    // コンストラクタ
    public SessionController(SessionService sessionService,
                            ClassroomRepository classroomRepository,
                            SessionRepository sessionRepository,
                            DepartmentRepository departmentRepository,
                            SubjectRepository subjectRepository,
                            AttendanceStatusRepository attendanceStatusRepository,
                            DepartmentSubjectRepository departmentSubjectRepository) {
        this.sessionService = sessionService;
        this.classroomRepository = classroomRepository;
        this.sessionRepository = sessionRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
        this.departmentSubjectRepository = departmentSubjectRepository;
    }

    //セッション画面の表示
    @GetMapping
    public String showSetupPage(Model model) {

        // 画面のセレクトボックスに必要なマスタデータを渡す
        model.addAttribute("classroomList", classroomRepository.findAll());
        model.addAttribute("subjectList", subjectRepository.findAll());
        model.addAttribute("departmentList", departmentRepository.findAll());
        
        // 今日の日付・現在時刻をセット
        model.addAttribute("defaultDate", LocalDate.now());
        model.addAttribute("defaultTime", LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
        
        return "session/session";
    }

    // --- API ---

    // 学年リスト取得API
    @GetMapping("/api/grades/{departmentId}")
    @ResponseBody
    public List<Integer> getGradesByDepartment(@PathVariable Integer departmentId) {
        return departmentSubjectRepository.findGradesByDepartmentId(departmentId);
    }

    // 科目リスト取得API
    @GetMapping("/api/subjects/{departmentId}")
    @ResponseBody
    public List<Map<String, Object>> getSubjectsByDepartment(@PathVariable Integer departmentId) {
        List<SubjectEntity> subjects = departmentSubjectRepository.findSubjectsByDepartmentId(departmentId);
        List<Map<String, Object>> result = new ArrayList<>();

        // Mapリストに変換
        for (SubjectEntity s : subjects) {
            Map<String, Object> map = new HashMap<>();
            map.put("subjectId", s.getSubjectId());
            map.put("subjectName", s.getSubjectName());
            result.add(map);
        }
        return result;
    }

    // 授業開始処理API
    @PostMapping("/start")
    @ResponseBody
    public Map<String, Object> startSession(@RequestBody Map<String, String> request) {

        // 画面から送られてきたパラメータを取得
        String dateStr = request.get("date");
        String timeStr = request.get("time");
        Integer classroomId = Integer.valueOf(request.get("classroomId"));
        Integer targetDepartmentId = Integer.valueOf(request.get("targetDepartmentId"));
        Integer targetGrade = Integer.valueOf(request.get("targetGrade"));
        String subjectName = request.get("subjectName");

        // 日時オブジェクトの生成
        LocalDate date = LocalDate.parse(dateStr);
        LocalTime time = LocalTime.parse(timeStr);
        LocalDateTime startDateTime = LocalDateTime.of(date, time);

        // セッションエンティティを作成
        SessionEntity session = new SessionEntity();
        session.setSessionDate(date);
        session.setStartTime(startDateTime);
        session.setActualClassroomId(classroomId);
        session.setTargetDepartmentId(targetDepartmentId);
        session.setTargetGrade(targetGrade);
        
        // 学科情報などを取得して、備考欄(Note)に「学科 クラス 学年 : 授業名」の形式で保存
        DepartmentEntity dept = departmentRepository.findById(targetDepartmentId).orElse(null);
        String deptName = (dept != null && dept.getMajor() != null) ? dept.getMajor().getMajorName() : "不明";
        String className = (dept != null) ? dept.getClassName() : "";
        
        session.setNote(deptName + " " + className + " " + targetGrade + "年 : " + subjectName);
        session.setSessionStatus(1); 

        // DBに保存
        SessionEntity savedSession = sessionRepository.save(session);

        // 作成されたSessionIDを返す（画面遷移用）
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", savedSession.getSessionId());
        return response;
    }

    // 実施中の授業画面を表示
    @GetMapping("/active/{sessionId}")
    public String showActiveSession(@PathVariable Integer sessionId, Model model) {

        // IDからセッション情報を取得
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid session Id:" + sessionId));

        // 教室名を取得
        ClassroomEntity classroom = classroomRepository.findById(session.getActualClassroomId())
                .orElse(null);
        String classroomName = (classroom != null) ? classroom.getClassroomName() : "不明な教室";

        // DTO作成
        Map<String, Object> sessionDto = new HashMap<>();
        sessionDto.put("sessionId", session.getSessionId());
        sessionDto.put("className", session.getNote()); 
        sessionDto.put("startTime", session.getStartTime());
        sessionDto.put("classroomName", classroomName);

        // HTMLへデータを渡す
        model.addAttribute("sessionDto", sessionDto);
        model.addAttribute("statusList", attendanceStatusRepository.findAll()); 

        return "session/sessionActive"; 
    }

    // 出席者リスト取得API
    @GetMapping("/api/attendees/{sessionId}")
    @ResponseBody
    public List<SessionDto> getAttendees(@PathVariable Integer sessionId) {
        return sessionService.getSessionAttendees(sessionId);
    }
    
    // 授業終了・確定処理API
    @PostMapping("/end")
    @ResponseBody
    public Map<String, String> endSession(@RequestBody EndSessionRequest request) {

        // DB保存・フラグ更新
        sessionService.endSession(request.sessionId, request.changes);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session ended and attendance finalized.");
        return response;
    }

    // 終了リクエストを受け取るためのデータ構造（DTO）
    public static class EndSessionRequest {
        public Integer sessionId;
        public Map<Integer, Integer> changes; // キー:学生ID, 値:ステータスID
    }
}