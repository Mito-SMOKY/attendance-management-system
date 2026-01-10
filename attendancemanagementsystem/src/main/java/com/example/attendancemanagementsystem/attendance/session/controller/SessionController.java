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

    // 初期表示
    @GetMapping
    public String showSetupPage(Model model, Principal principal) {

        // ログイン中のユーザ情報を取得
        UsersEntity user = usersRepository.findByLoginId(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // モデルに必要なデータを追加
        model.addAttribute("userId", user.getUserId());
        model.addAttribute("subjectList", subjectRepository.findAll());
        model.addAttribute("classroomList", classroomRepository.findAll());
        model.addAttribute("timeSlotList", timeSlotRepository.findAll());
        model.addAttribute("majorList", majorRepository.findAll());
        model.addAttribute("defaultDate", LocalDate.now());
        
        return "session/session";
    }

    // 時間割反映API
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

    // 授業開始API
    @PostMapping("/start")
    @ResponseBody
    public ResponseEntity<?> startSession(@RequestBody StartSessionDto request, Principal principal) {
        try {

            // ログイン中のユーザ情報を取得
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

            // 成功したらセッションIDを返す
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getSessionId());
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            
            // 既に実施中のセッションがある場合
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

    // 強制終了API
    @PostMapping("/api/force-end")
    @ResponseBody
    public ResponseEntity<String> forceEndSession(@RequestBody Map<String, Integer> payload) {

        // セッションIDをペイロードから取得
        Integer sessionId = payload.get("sessionId");
        if (sessionId == null) {
            return ResponseEntity.badRequest().body("Session ID is required");
        }
        
        // 強制終了処理を実行
        try {
            sessionService.forceEndSession(sessionId);
            return ResponseEntity.ok("Force ended successfully");
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error forcing end session: " + e.getMessage());
        }
    }

    // 実施中授業画面表示
    @GetMapping("/active/{sessionId}")
    public String showActiveSession(@PathVariable Integer sessionId, Model model) {

        // セッション情報を取得してDTOに変換
        SessionEntity session = sessionService.getSession(sessionId);
        SessionDto sessionDto = sessionDtoMapper.toDto(session);

        // モデルに追加
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
    
    // 実施中セッション確認API
    @GetMapping("/api/active/check")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> checkActiveSession(@RequestParam("userId") Integer userId) {
        
        // 実施中セッションをユーザIDで検索
        return sessionService.findActiveSessionByUserId(userId)
            .map(session -> {

                // 見つかった場合: セッションIDを返す
                Map<String, Integer> response = new HashMap<>();
                response.put("sessionId", session.getSessionId());
                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                
                // 見つからなかった場合: 404を返す
                return ResponseEntity.notFound().build();
            });
    }
    
    // 授業終了API
    @PostMapping("/end")
    @ResponseBody
    public Map<String, String> endSession(@RequestBody EndSessionDto request) {

        // 授業終了処理を実行
        sessionService.endSession(request.getSessionId(), request.getChanges());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session ended.");
        return response;
    }

    // 授業取消API
    @PostMapping("/cancel")
    @ResponseBody
    public Map<String, String> cancelSession(@RequestBody Map<String, Integer> payload) {

        // セッションIDをペイロードから取得
        Integer sessionId = payload.get("sessionId");
        sessionService.cancelSession(sessionId);

        // 処理完了メッセージを返す
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session cancelled.");
        return response;
    }

    // 学科 -> コース
    @GetMapping("/api/options/courses")
    @ResponseBody
    public List<Map<String, Object>> getCourses(@RequestParam("majorId") Integer majorId) {

        // 指定された学科IDからコース情報を取得
        MajorEntity major = majorRepository.findById(majorId).orElse(null);
        
        // コース情報をマップ形式で返す
        if (major != null && major.getCourse() != null) {
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("courseId", major.getCourse().getCourseId());
            courseMap.put("courseName", major.getCourse().getCourseName());
            return List.of(courseMap);
        }
        return List.of();
    }

    // コース -> 学年
    @GetMapping("/api/options/grades")
    @ResponseBody
    public List<Integer> getGrades(@RequestParam("courseId") Integer courseId) {

        // 指定されたコースIDから学年一覧を取得して返す
        return enrollmentsRepository.findDistinctGradesByCourseId(courseId);
    }

    // コース + 学年 -> クラス
    @GetMapping("/api/options/classes")
    @ResponseBody
    public List<Map<String, Object>> getClasses(
            @RequestParam("courseId") Integer courseId, 
            @RequestParam("grade") Integer grade) {
        
        // 指定されたコースIDと学年から学科IDとクラス名の組み合わせを取得
        List<Object[]> results = enrollmentsRepository.findDistinctDepartmentIdAndClass(courseId, grade);
        List<Map<String, Object>> responseList = new ArrayList<>();
        
        // 結果をマップ形式に変換してリストに追加
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("departmentId", row[0]);
            map.put("className", row[1]);
            responseList.add(map);
        }

        return responseList;
    }
}