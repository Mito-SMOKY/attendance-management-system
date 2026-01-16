package com.example.attendancemanagementsystem.user.admin.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.*;
import com.example.attendancemanagementsystem.common.repository.*;
import com.example.attendancemanagementsystem.user.admin.dto.ClassInfoDto;
import com.example.attendancemanagementsystem.user.admin.dto.ClassInfoDto.StudentDetail;

@Service
public class AdminClassInfoService {

    private final SessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final UsersRepository usersRepository;
    private final AttendanceStatusRepository attendanceStatusRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final TimeSlotRepository timeSlotRepository;

    // 依存するリポジトリの注入
    public AdminClassInfoService(
            SessionRepository sessionRepository,
            StudentRepository studentRepository,
            AttendanceRepository attendanceRepository,
            UsersRepository usersRepository,
            AttendanceStatusRepository attendanceStatusRepository,
            SubjectRepository subjectRepository,
            ClassroomRepository classroomRepository,
            TimeSlotRepository timeSlotRepository) {
        
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.usersRepository = usersRepository;
        this.attendanceStatusRepository = attendanceStatusRepository;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    // 授業詳細画面の表示用データ取得
    @Transactional(readOnly = true)
    public ClassInfoDto getClassInfo(Integer sessionId) {
        ClassInfoDto dto = new ClassInfoDto();

        // 指定されたセッションIDの授業情報取得
        SessionEntity session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return new ClassInfoDto();
        dto.setSessionId(sessionId);

        // 教科情報をセット
        if (session.getSubject() != null) {
            dto.setSubjectName(session.getSubject().getSubjectName());
            dto.setCurrentSubjectId(session.getSubject().getSubjectId());
        }
        
        // 担当教員情報をセット
        if (session.getUserId() != null) {
            dto.setCurrentTeacherId(session.getUserId());
            usersRepository.findById(session.getUserId()).ifPresent(u -> {
                dto.setTeacherName(u.getName());
                dto.setTeacherLoginId(u.getLoginId()); 
            });
        } else {
            dto.setTeacherName("未設定");
        }

        // 教室情報をセット
        if (session.getClassroom() != null) {
            dto.setClassroomName(session.getClassroom().getClassroomName());
            dto.setCurrentClassroomId(session.getClassroom().getClassroomId());
        }
        
        // 学科・コース情報をセット
        if (session.getDepartment() != null && session.getDepartment().getMajor() != null && session.getDepartment().getMajor().getCourse() != null) {
            dto.setCourseName(session.getDepartment().getMajor().getCourse().getCourseName());
        }
        
        // 実施日情報をフォーマットしてセット
        if (session.getSessionDate() != null) {
            dto.setDateText(session.getSessionDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd(E)", Locale.JAPANESE)));
            dto.setCurrentDateValue(session.getSessionDate().toString());
        }
        
        // 時間割（時限）情報をセット
        if (session.getTimeSlot() != null) {
            dto.setTimeSlotText(session.getTimeSlot().getStartTime().toString().substring(0, 5) + " ～ " + session.getTimeSlot().getEndTime().toString().substring(0, 5));
            dto.setCurrentTimeSlotId(session.getTimeSlot().getSlotId());
        }

        // 編集用プルダウンのための全マスタデータを取得
        dto.setAllSubjects(subjectRepository.findAll());
        dto.setAllClassrooms(classroomRepository.findAll());
        dto.setAllTimeSlots(timeSlotRepository.findAllByOrderBySlotIdAsc());
        dto.setAllTeachers(usersRepository.findAll());
        dto.setAllStatuses(attendanceStatusRepository.findAll());

        // 受講生徒リストの生成処理を開始
        List<StudentDetail> studentDetails = new ArrayList<>();
        if (session.getDepartment() != null && session.getTargetGrade() != null) {
            Integer deptId = session.getDepartment().getDepartmentId();
            Integer targetGrade = session.getTargetGrade(); 
            Integer currentYear = calculateAcademicYear(session.getSessionDate());

            // 対象学科・学年の全生徒を取得
            List<StudentEntity> allStudents = studentRepository.findByDepartmentAndGrade(deptId, targetGrade, currentYear);
            
            // 既に登録されている出欠情報を取得しマップ化
            List<AttendanceEntity> attendanceList = attendanceRepository.findBySessionId(sessionId);
            Map<Integer, AttendanceEntity> attendanceMap = new HashMap<>();
            for (AttendanceEntity att : attendanceList) {
                if (att.getStudent() != null) attendanceMap.put(att.getStudent().getUserId(), att);
            }

            // 生徒ごとに詳細情報をセット
            for (StudentEntity s : allStudents) {
                StudentDetail detail = new StudentDetail();
                detail.setUserId(s.getUserId());
                
                // 生徒の基本情報をセット
                if (s.getUsers() != null) {
                    detail.setName(s.getUsers().getName());
                    detail.setStudentNumber(s.getUsers().getLoginId());
                } else {
                    detail.setName("不明");
                    detail.setStudentNumber("-");
                }
                
                // 学籍情報の詳細をセット
                if (s.getEnrollments() != null) {
                    for (EnrollmentsEntity e : s.getEnrollments()) {
                        if (e.getDepartment() != null && e.getDepartment().getDepartmentId().equals(deptId) && e.getGrade().equals(targetGrade)) {
                            detail.setGrade(String.valueOf(e.getGrade()));
                            if (e.getDepartment().getClassName() != null) detail.setClassName(e.getDepartment().getClassName());
                            break;
                        }
                    }
                }
                if (detail.getClassName() == null) detail.setClassName("-");
                if (detail.getGrade() == null) detail.setGrade(String.valueOf(targetGrade));

                // 出欠状況をセット（登録済みならそのステータス、なければ未登録）
                if (attendanceMap.containsKey(s.getUserId())) {
                    AttendanceEntity att = attendanceMap.get(s.getUserId());
                    if (att.getStatus() != null) {
                        detail.setStatusLabel(mapStatusToLabel(att.getStatus().getStatusName()));
                        detail.setStatusStyle(mapStatusToStyle(att.getStatus().getStatusName()));
                        detail.setCurrentStatusId(att.getStatus().getStatusId());
                    }
                } else {
                    detail.setStatusLabel("-");
                    detail.setStatusStyle("");
                }
                studentDetails.add(detail);
            }
        }
        dto.setStudents(studentDetails);
        return dto;
    }

    // 授業情報の更新
    @Transactional
    public void updateSessionInfo(Integer sessionId, Map<String, String> updateData) {

        // 更新対象のセッションを取得
        SessionEntity session = sessionRepository.findById(sessionId).orElseThrow(() -> new RuntimeException("Session not found"));
        
        // 画面からの入力値を取得
        String subjectId = updateData.get("subjectId");
        String teacherId = updateData.get("teacherId");
        String classroomId = updateData.get("classroomId");
        String dateStr = updateData.get("sessionDate");
        String slotId = updateData.get("timeSlotId");

        // 各フィールドの値があればエンティティを更新
        if (subjectId != null && !subjectId.isEmpty()) {
            SubjectEntity s = new SubjectEntity(); s.setSubjectId(Integer.parseInt(subjectId));
            session.setSubject(s);
        }
        if (teacherId != null && !teacherId.isEmpty()) session.setUserId(Integer.parseInt(teacherId));
        if (classroomId != null && !classroomId.isEmpty()) {
            ClassroomEntity c = new ClassroomEntity(); c.setClassroomId(Integer.parseInt(classroomId));
            session.setClassroom(c);
        }
        if (dateStr != null && !dateStr.isEmpty()) session.setSessionDate(LocalDate.parse(dateStr));
        if (slotId != null && !slotId.isEmpty()) {
            TimeSlotEntity t = new TimeSlotEntity(); t.setSlotId(Integer.parseInt(slotId));
            session.setTimeSlot(t);
        }
        
        // 変更を保存
        sessionRepository.save(session);
    }

    // 生徒の出欠情報を一括更新
    @Transactional
    public void updateAttendance(Integer sessionId, Map<String, String> updateMap) {

        // 既存の出欠データを取得してマップ化
        List<AttendanceEntity> existingList = attendanceRepository.findBySessionId(sessionId);
        Map<Integer, AttendanceEntity> existingMap = new HashMap<>();
        for (AttendanceEntity att : existingList) existingMap.put(att.getStudent().getUserId(), att);

        // 送信された各生徒のデータを処理
        for (Map.Entry<String, String> entry : updateMap.entrySet()) {
            try {
                Integer userId = Integer.parseInt(entry.getKey());
                Integer statusId = Integer.parseInt(entry.getValue());

                // 既存データがあれば更新、なければ新規作成
                AttendanceEntity entity;
                if (existingMap.containsKey(userId)) {
                    entity = existingMap.get(userId);
                } else {
                    entity = new AttendanceEntity();
                    entity.setSessionId(sessionId);
                    StudentEntity student = new StudentEntity();
                    student.setUserId(userId);
                    entity.setStudent(student);
                }
                
                // ステータスIDをセットして保存
                AttendanceStatusEntity status = new AttendanceStatusEntity();
                status.setStatusId(statusId);
                entity.setStatusId(status);
                attendanceRepository.save(entity);
            } catch (Exception e) { continue; }
        }
    }

    // 画面表示用の記号に変換
    private String mapStatusToLabel(String dbStatusName) {
        if (dbStatusName == null) return "-";
        switch (dbStatusName) {
            case "出席": return "○";
            case "欠席": return "×";
            case "遅刻": return "△";
            case "公欠": return "公";
            default: return dbStatusName;
        }
    }
    
    // 出欠ステータスに応じたCSSクラス名を返却
    private String mapStatusToStyle(String dbStatusName) {
        if (dbStatusName == null) return "";
        switch (dbStatusName) {
            case "出席": return "status-present";
            case "公欠": return "status-present";
            case "欠席": return "status-absent";
            case "遅刻": return "status-late";
            default: return "";
        }
    }
    
    // 日付から該当する年度を計算（1〜3月は前年度扱い）
    private Integer calculateAcademicYear(LocalDate date) {
        if (date == null) return 2025;
        int year = date.getYear();
        int month = date.getMonthValue();
        if (month <= 3) return year - 1;
        return year;
    }
}