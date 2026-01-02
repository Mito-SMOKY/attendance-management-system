package com.example.attendancemanagementsystem.attendance.session.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

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
    private final UsersRepository usersRepository;

    public SessionController(SessionService sessionService,
                            ClassroomRepository classroomRepository,
                            SessionRepository sessionRepository,
                            DepartmentRepository departmentRepository,
                            SubjectRepository subjectRepository,
                            AttendanceStatusRepository attendanceStatusRepository,
                            DepartmentSubjectRepository departmentSubjectRepository,
                            UsersRepository usersRepository) {
        this.sessionService = sessionService;
        this.classroomRepository = classroomRepository;
        this.sessionRepository = sessionRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
        this.departmentSubjectRepository = departmentSubjectRepository;
        this.usersRepository = usersRepository;
    }

    // 授業開始画面の表示
    @GetMapping
    public String showSetupPage(Model model, Principal principal) {
        
        // 現在アクティブなセッションがあれば取得
        SessionEntity activeSession = sessionRepository.findFirstBySessionStatusOrderBySessionIdDesc(1);
        if (activeSession != null) {
            model.addAttribute("activeSession", activeSession);
        }

        // マスタデータ
        model.addAttribute("classroomList", classroomRepository.findAll());
        model.addAttribute("subjectList", subjectRepository.findAll());
        model.addAttribute("departmentList", departmentRepository.findAll());
        
        // 初期値（日付・時刻）
        model.addAttribute("defaultDate", LocalDate.now());
        model.addAttribute("defaultTime", LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
        
        // 今の時間に該当する授業があれば、それを初期値として渡す
        if (principal != null) {
            UsersEntity user = usersRepository.findByLoginId(principal.getName()).orElse(null);
            if (user != null) {

                // S現在時刻判定
                Optional<TimetableEntity> currentLesson = sessionService.findCurrentScheduledLesson(user.getUserId());
                
                if (currentLesson.isPresent()) {
                    TimetableEntity lesson = currentLesson.get();

                    // 画面のセレクトボックスを自動選択させるための値をセット
                    model.addAttribute("suggestedDeptId", lesson.getDepartment().getDepartmentId());
                    model.addAttribute("suggestedSubjectId", lesson.getSubjectId());
                    model.addAttribute("suggestedGrade", 1); 
                }
            }
        }
        
        return "session/session";
    }
    // 学年リストAPI
    @GetMapping("/api/grades/{departmentId}")
    @ResponseBody
    public List<Integer> getGradesByDepartment(@PathVariable Integer departmentId) {
        return departmentSubjectRepository.findGradesByDepartmentId(departmentId);
    }

    // 科目リストAPI
    @GetMapping("/api/subjects/{departmentId}")
    @ResponseBody
    public List<Map<String, Object>> getSubjectsByDepartment(@PathVariable Integer departmentId) {
        List<SubjectEntity> subjects = departmentSubjectRepository.findSubjectsByDepartmentId(departmentId);
        List<Map<String, Object>> result = new ArrayList<>();
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
    public Map<String, Object> startSession(@RequestBody Map<String, String> request, Principal principal) {

        // パラメータ取得
        String dateStr = request.get("date");
        String timeStr = request.get("time");
        Integer classroomId = Integer.valueOf(request.get("classroomId"));
        Integer targetDepartmentId = Integer.valueOf(request.get("targetDepartmentId"));
        Integer targetGrade = Integer.valueOf(request.get("targetGrade"));
        String subjectName = request.get("subjectName");
        Integer subjectId = Integer.valueOf(request.get("subjectId")); 
        LocalDate date = LocalDate.parse(dateStr);
        LocalTime time = LocalTime.parse(timeStr);
        LocalDateTime startDateTime = LocalDateTime.of(date, time);

        // セッション情報の仮組み
        SessionEntity session = new SessionEntity();
        session.setSessionDate(date);
        session.setStartTime(startDateTime);
        session.setActualClassroomId(classroomId);
        session.setTargetDepartmentId(targetDepartmentId);
        session.setTargetGrade(targetGrade);
        
        // 備考欄の作成
        DepartmentEntity dept = departmentRepository.findById(targetDepartmentId).orElse(null);
        String deptName = (dept != null && dept.getMajor() != null) ? dept.getMajor().getMajorName() : "不明";
        String className = (dept != null) ? dept.getClassName() : "";
        session.setNote(deptName + " " + className + " " + targetGrade + "年 : " + subjectName);
        session.setSessionStatus(1); 

        // ログインユーザーの特定
        Integer teacherUserId = null;
        if (principal != null) {
            UsersEntity user = usersRepository.findByLoginId(principal.getName()).orElse(null);
            if (user != null) {
                teacherUserId = user.getUserId();
            }
        }
        
        // ログインしていない場合
        if (teacherUserId == null) {
            teacherUserId = 1; 
        }

        //  時間割データの検索と紐付け
        Optional<TimetableEntity> scheduledLesson = sessionService.findTimetableBySchedule(teacherUserId, date, subjectId);
        
        if (scheduledLesson.isPresent()) {
            session.setTimeTable(scheduledLesson.get());
        } else {
            session.setTimeTable(null);
        }

        // 保存
        SessionEntity savedSession = sessionRepository.save(session);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", savedSession.getSessionId());
        return response;
    }

    // 授業中画面の表示
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
        model.addAttribute("statusList", attendanceStatusRepository.findAll()); 

        return "session/sessionActive"; 
    }

    // 出席者リストAPI
    @GetMapping("/api/attendees/{sessionId}")
    @ResponseBody
    public List<SessionDto> getAttendees(@PathVariable Integer sessionId) {
        return sessionService.getSessionAttendees(sessionId);
    }
    
    // 授業終了API
    @PostMapping("/end")
    @ResponseBody
    public Map<String, String> endSession(@RequestBody EndSessionRequest request) {
        sessionService.endSession(request.sessionId, request.changes);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session ended and attendance finalized.");
        return response;
    }

    @PostMapping("/cancel")
    @ResponseBody
    public Map<String, String> cancelSession(@RequestBody Map<String, Integer> payload) {
        Integer sessionId = payload.get("sessionId");
        sessionService.cancelSession(sessionId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session cancelled and deleted.");
        return response;
    }

    public static class EndSessionRequest {
        public Integer sessionId;
        public Map<Integer, Integer> changes;
    }
}