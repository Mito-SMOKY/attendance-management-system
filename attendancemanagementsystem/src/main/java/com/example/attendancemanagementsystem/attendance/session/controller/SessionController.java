package com.example.attendancemanagementsystem.attendance.session.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.attendancemanagementsystem.attendance.session.dto.EndSessionDto;
import com.example.attendancemanagementsystem.attendance.session.dto.SessionDto;
import com.example.attendancemanagementsystem.attendance.session.dto.StartSessionDto;
import com.example.attendancemanagementsystem.attendance.session.mapper.SessionDtoMapper;
import com.example.attendancemanagementsystem.attendance.session.service.SessionService;
import com.example.attendancemanagementsystem.common.entity.MajorEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.CourseRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.MajorRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimeSlotRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Controller
@RequestMapping("/session")
public class SessionController {

    private final SessionService sessionService;
    private final SessionDtoMapper sessionDtoMapper;
    private final ClassroomRepository classroomRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceStatusRepository attendanceStatusRepository;
    private final UsersRepository usersRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final EnrollmentsRepository enrollmentsRepository;
    private final TimetableRepository timetableRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final MajorRepository majorRepository;

    public SessionController(SessionService sessionService,
                             SessionDtoMapper sessionDtoMapper,
                             ClassroomRepository classroomRepository,
                             SubjectRepository subjectRepository,
                             AttendanceStatusRepository attendanceStatusRepository,
                             UsersRepository usersRepository,
                             TimeSlotRepository timeSlotRepository,
                             EnrollmentsRepository enrollmentsRepository,
                             TimetableRepository timetableRepository,
                             DepartmentRepository departmentRepository,
                             CourseRepository courseRepository,
                             MajorRepository majorRepository) {
        this.sessionService = sessionService;
        this.sessionDtoMapper = sessionDtoMapper;
        this.classroomRepository = classroomRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
        this.usersRepository = usersRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.timetableRepository = timetableRepository;
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.majorRepository = majorRepository;
    }

    @GetMapping
    public String showSetupPage(Model model, Principal principal) {
        UsersEntity user = usersRepository.findByLoginId(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        model.addAttribute("userId", user.getUserId());
        
        // アクティブセッションがあればモデルに追加（HTML側でポップアップ表示に使用）
        sessionService.findActiveSessionByUserId(user.getUserId())
                .ifPresent(s -> model.addAttribute("activeSession", s));
        
        model.addAttribute("subjectList", subjectRepository.findAll());
        model.addAttribute("classroomList", classroomRepository.findAll());
        model.addAttribute("timeSlotList", timeSlotRepository.findAll());
        model.addAttribute("majorList", majorRepository.findAll());
        model.addAttribute("defaultDate", LocalDate.now());
        
        return "session/session";
    }

    @GetMapping("/api/timetable/get")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTimetable(
            @RequestParam("userId") Integer userId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("slotId") Integer slotId) {

        Map<String, Object> result = timetableRepository.findSimpleTimetableData(userId, date, slotId);
        
        if (result != null && !result.isEmpty()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/start")
    @ResponseBody
    public ResponseEntity<?> startSession(@RequestBody StartSessionDto request, Principal principal) {
        try {
            UsersEntity user = usersRepository.findByLoginId(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            SessionEntity session = sessionService.startSession(
                user.getUserId(), 
                request.getSubjectId(), 
                request.getDate(), 
                request.getSlotId(),
                request.getClassroomId(), 
                request.getDepartmentId(), 
                request.getTargetGrade()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getSessionId());
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // Serviceで投げた「違う時限の授業が進行中」というエラーをキャッチ
            // 409 Conflict とエラーメッセージを返す (JS側でalert表示に使用)
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("システムエラーが発生しました: " + e.getMessage());
        }
    }

    // ★追加: 強制終了API (HTMLのポップアップから呼ばれる)
    @PostMapping("/api/force-end")
    @ResponseBody
    public ResponseEntity<String> forceEndSession(@RequestBody Map<String, Integer> payload) {
        Integer sessionId = payload.get("sessionId");
        if (sessionId == null) {
            return ResponseEntity.badRequest().body("Session ID is required");
        }
        
        try {
            sessionService.forceEndSession(sessionId);
            return ResponseEntity.ok("Force ended successfully");
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error forcing end session: " + e.getMessage());
        }
    }

    @GetMapping("/active/{sessionId}")
    public String showActiveSession(@PathVariable Integer sessionId, Model model) {
        SessionEntity session = sessionService.getSession(sessionId);
        SessionDto sessionDto = sessionDtoMapper.toDto(session);

        model.addAttribute("sessionDto", sessionDto);
        model.addAttribute("statusList", attendanceStatusRepository.findAll()); 

        return "session/sessionActive"; 
    }

    @GetMapping("/api/attendees/{sessionId}")
    @ResponseBody
    public List<SessionDto> getAttendees(@PathVariable Integer sessionId) {
        return sessionService.getSessionAttendees(sessionId);
    }
    
    @PostMapping("/end")
    @ResponseBody
    public Map<String, String> endSession(@RequestBody EndSessionDto request) {
        sessionService.endSession(request.getSessionId(), request.getChanges());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session ended.");
        return response;
    }

    @PostMapping("/cancel")
    @ResponseBody
    public Map<String, String> cancelSession(@RequestBody Map<String, Integer> payload) {
        Integer sessionId = payload.get("sessionId");
        sessionService.cancelSession(sessionId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session cancelled.");
        return response;
    }

    // --- 連動プルダウン用API ---

    // ① 学科(Major) -> コース(Course)
    @GetMapping("/api/options/courses")
    @ResponseBody
    public List<Map<String, Object>> getCourses(@RequestParam("majorId") Integer majorId) {
        MajorEntity major = majorRepository.findById(majorId).orElse(null);
        
        if (major != null && major.getCourse() != null) {
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("courseId", major.getCourse().getCourseId());
            courseMap.put("courseName", major.getCourse().getCourseName());
            return List.of(courseMap);
        }
        return List.of();
    }

    // ② コース(Course) -> 学年(Grade)
    @GetMapping("/api/options/grades")
    @ResponseBody
    public List<Integer> getGrades(@RequestParam("courseId") Integer courseId) {
        return enrollmentsRepository.findDistinctGradesByCourseId(courseId);
    }

    // ③ コース(Course) + 学年(Grade) -> クラス(IDと名前)
    @GetMapping("/api/options/classes")
    @ResponseBody
    public List<Map<String, Object>> getClasses(
            @RequestParam("courseId") Integer courseId, 
            @RequestParam("grade") Integer grade) {
        
        List<Object[]> results = enrollmentsRepository.findDistinctDepartmentIdAndClass(courseId, grade);
        
        List<Map<String, Object>> responseList = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("departmentId", row[0]);
            map.put("className", row[1]);
            responseList.add(map);
        }

        return responseList;
    }
}