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
//日次出席詳細データを提供するサービスクラス
public class AttendanceDisplayService {

    private final UsersRepository usersRepository;
    private final StudentRepository studentRepository;
    private final TimetableRepository timetableRepository;
    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;
    // セッションリポジトリを追加
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

    //  * 指定した日付の日次詳細データを取得する
    public List<DailyAttendanceDto> getDailyAttendanceDetails(String loginId, LocalDate date) {
        List<DailyAttendanceDto> result = new ArrayList<>();

        // 1. ユーザー・生徒情報取得
        UsersEntity user = usersRepository.findByLoginId(loginId).orElseThrow();
        StudentEntity student = studentRepository.findByUsers(user).orElseThrow();

        // 2. 所属学科IDを特定 (今回は仮で1固定)
        Integer deptId = 1; 

        // 3. その日の基本時間割を取得
        List<TimetableEntity> timetables = timetableRepository.findByDateAndDepartment_DepartmentIdOrderBySlotId(date, deptId);

        // ★追加: その日のセッション(実施授業)を取得
        List<SessionEntity> sessions = sessionRepository.findByDepartment_DepartmentIdAndSessionDate(deptId, date);
        
        // SlotIDをキーにしてMap化
        Map<Integer, SessionEntity> sessionMap = new HashMap<>();
        for (SessionEntity s : sessions) {
            if (s.getTimeSlot() != null) {
                sessionMap.put(s.getTimeSlot().getSlotId(), s);
            }
        }

        // 4. 時間割ごとにループしてDTOを作成
        for (TimetableEntity tt : timetables) {
            
            String subjectName;
            String classroomName;
            String statusSymbol = "-"; 
            
            Integer displaySubjectId = tt.getSubjectId();

            // ★対応するセッションがあるか確認
            SessionEntity session = sessionMap.get(tt.getSlotId());

            if (session != null) {
                // --- セッション(実施後)の場合 ---
                // 科目名・教室名をセッションから取得
                subjectName = (session.getSubject() != null) 
                        ? session.getSubject().getSubjectName() 
                        : "科目ID:" + tt.getSubjectId(); // フォールバック
                
                if (session.getSubject() != null) {
                    displaySubjectId = session.getSubject().getSubjectId();
                }

                classroomName = (session.getClassroom() != null) 
                        ? session.getClassroom().getClassroomName() 
                        : "教室ID:" + tt.getClassroomId();

                // 出席情報を取得 (StudentとSessionで検索)
                // ※AttendanceRepositoryに findByStudentAndSession メソッドが必要です
                Optional<AttendanceEntity> attOpt = attendanceRepository.findByStudentAndSession(student, session);

                if (attOpt.isPresent()) {
                    String statusName = attOpt.get().getStatus().getStatusName();
                    if ("出席".equals(statusName)) statusSymbol = "〇";
                    else if ("欠席".equals(statusName)) statusSymbol = "×";
                    else if ("遅刻".equals(statusName)) statusSymbol = "△";
                    else statusSymbol = statusName; 
                } else {
                    // セッションはあるが生徒の出席レコードがない場合（未スキャンなど）
                    statusSymbol = "-"; 
                }

            } else {
                // --- セッションなし(実施前/予定)の場合 ---
                subjectName = subjectRepository.findById(tt.getSubjectId())
                        .map(s -> s.getSubjectName()).orElse("科目ID:" + tt.getSubjectId());
                
                classroomName = classroomRepository.findById(tt.getClassroomId())
                        .map(c -> c.getClassroomName()).orElse("教室ID:" + tt.getClassroomId());
                
                // 実施前なのでステータスは "-"
                statusSymbol = "-";
            }

            // DTOに追加
            result.add(new DailyAttendanceDto(
                displaySubjectId,
                tt.getSlotId(),
                subjectName,
                classroomName,
                statusSymbol
            ));
        }
        
        return result;
    }
}