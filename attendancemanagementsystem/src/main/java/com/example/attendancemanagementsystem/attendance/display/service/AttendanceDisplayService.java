package com.example.attendancemanagementsystem.attendance.display.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.display.dto.DailyAttendanceDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
@Transactional(readOnly = true)
public class AttendanceDisplayService {

    private final UsersRepository usersRepository;
    private final StudentRepository studentRepository;
    private final TimetableRepository timetableRepository;
    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    private final SessionRepository sessionRepository;

    public AttendanceDisplayService(
            UsersRepository usersRepository,
            StudentRepository studentRepository,
            TimetableRepository timetableRepository,
            AttendanceRepository attendanceRepository,
            SubjectRepository subjectRepository,
            ClassroomRepository classroomRepository,
            SessionRepository sessionRepository) {
        this.usersRepository = usersRepository;
        this.studentRepository = studentRepository;
        this.timetableRepository = timetableRepository;
        this.attendanceRepository = attendanceRepository;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
        this.sessionRepository = sessionRepository;
    }

    // 指定した日付の日次詳細データを取得する
    public List<DailyAttendanceDto> getDailyAttendanceDetails(String loginId, LocalDate date) {
        List<DailyAttendanceDto> result = new ArrayList<>();

        // 1. ユーザー・生徒情報取得
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found: " + loginId));
        StudentEntity student = studentRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Student not found for user: " + loginId));

        // 2. 所属学科IDと学年を特定
        EnrollmentsEntity activeEnrollment = student.getEnrollments().stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsActive()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Active enrollment not found for student: " + student.getUserId()));

        Integer deptId = activeEnrollment.getDepartmentId();
        Integer grade = activeEnrollment.getGrade();

        // 3. データの取得
        
        // (A) 予定: その日の学科・学年の基本時間割を取得
        List<TimetableEntity> timetables = timetableRepository.findByDepartment_DepartmentIdAndGradeAndDateOrderBySlotIdAsc(
                deptId, grade, date);

        // (B) 実績: その日の学科の実施セッション(SessionFlag=true)のみを取得
        List<SessionEntity> sessions = sessionRepository.findByDepartment_DepartmentIdAndSessionDateAndSessionFlagTrue(deptId, date);
        
        // Map化
        Map<Integer, TimetableEntity> timetableMap = new HashMap<>();
        for (TimetableEntity tt : timetables) {
            if (tt.getSlotId() != null) {
                timetableMap.put(tt.getSlotId(), tt);
            }
        }

        Map<Integer, SessionEntity> sessionMap = new HashMap<>();
        for (SessionEntity s : sessions) {
            if (s.getTimeSlot() != null && s.getTargetGrade() != null && s.getTargetGrade().equals(grade)) {
                sessionMap.put(s.getTimeSlot().getSlotId(), s);
            }
        }

        // 4. ループ処理
        for (int i = 1; i <= 4; i++) {
            Integer currentSlot = i;
            
            TimetableEntity tt = timetableMap.get(currentSlot);
            SessionEntity session = sessionMap.get(currentSlot);

            if (tt == null && session == null) {
                result.add(new DailyAttendanceDto(null, currentSlot, "-", "-", "-"));
                continue;
            }

            String subjectName = "-";
            String classroomName = "-";
            String statusSymbol = "-";
            Integer displaySubjectId = null;

            if (session != null) {
                // --- Session (実績) がある場合 ---
                
                if (session.getSubject() != null) {
                    subjectName = session.getSubject().getSubjectName();
                    displaySubjectId = session.getSubject().getSubjectId();
                } else {
                    subjectName = "科目ID:" + (tt != null ? tt.getSubjectId() : "?");
                }

                if (session.getClassroom() != null) {
                    classroomName = session.getClassroom().getClassroomName();
                } else {
                     classroomName = "未定";
                }

                // 出席情報を取得
                Optional<AttendanceEntity> attOpt = attendanceRepository.findByStudentAndSession(student, session);

                if (attOpt.isPresent() && attOpt.get().getStatus() != null) {
                    String statusName = attOpt.get().getStatus().getStatusName();
                    
                    // ★ここを修正: DBから取得した文字列の空白を除去して判定する
                    if (statusName != null) {
                        statusName = statusName.trim();
                    } else {
                        statusName = "";
                    }
                    
                    if ("出席".equals(statusName)) {
                        statusSymbol = "◎";
                    } else if ("公欠".equals(statusName)) {
                        statusSymbol = "〇";
                    } else if ("欠席".equals(statusName)) {
                        statusSymbol = "×";
                    } else if ("遅刻".equals(statusName) || "早退".equals(statusName)) {
                        statusSymbol = "△";
                    } else if ("出席停止".equals(statusName)) {
                        statusSymbol = "停";
                    } else {
                        // その他はそのまま表示
                        statusSymbol = statusName;
                    }
                    
                } else {
                    // SessionはあるがAttendanceがない場合
                    statusSymbol = "-";
                }

            } else {
                // --- Sessionがなく Timetable (予定) のみの場合 ---
                
                if (tt.getSubject() != null) {
                    subjectName = tt.getSubject().getSubjectName();
                } else {
                     subjectName = subjectRepository.findById(tt.getSubjectId())
                            .map(s -> s.getSubjectName()).orElse("科目ID:" + tt.getSubjectId());
                }
                displaySubjectId = tt.getSubjectId();

                if (tt.getClassroom() != null) {
                    classroomName = tt.getClassroom().getClassroomName();
                } else {
                    classroomName = classroomRepository.findById(tt.getClassroomId())
                            .map(c -> c.getClassroomName()).orElse("教室ID:" + tt.getClassroomId());
                }
                
                statusSymbol = "-";
            }

            result.add(new DailyAttendanceDto(
                displaySubjectId,
                currentSlot,
                subjectName,
                classroomName,
                statusSymbol
            ));
        }
        
        return result;
    }
}