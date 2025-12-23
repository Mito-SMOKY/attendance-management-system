package com.example.attendancemanagementsystem.attendance.display.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.display.dto.DailyAttendanceDto;
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
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

    // @Autowired
    public AttendanceDisplayService(
            UsersRepository usersRepository,
            StudentRepository studentRepository,
            TimetableRepository timetableRepository,
            AttendanceRepository attendanceRepository,
            SubjectRepository subjectRepository,
            ClassroomRepository classroomRepository) {
        this.usersRepository = usersRepository;
        this.studentRepository = studentRepository;
        this.timetableRepository = timetableRepository;
        this.attendanceRepository = attendanceRepository;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
    }

    //  * 指定した日付の日次詳細データを取得する
    public List<DailyAttendanceDto> getDailyAttendanceDetails(String loginId, LocalDate date) {
        List<DailyAttendanceDto> result = new ArrayList<>();

        // 1. ユーザー・生徒情報取得
        UsersEntity user = usersRepository.findByLoginId(loginId).orElseThrow();
        StudentEntity student = studentRepository.findByUsers(user).orElseThrow();

        // 2. 所属学科IDを特定 (今回は仮で1固定)
        Integer deptId = 1; 

        // 3. その日の時間割を取得
        List<TimetableEntity> timetables = timetableRepository.findByDateAndDepartment_DepartmentIdOrderBySlotId(date, deptId);

        // 4. 時間割ごとにループしてDTOを作成
        for (TimetableEntity tt : timetables) {
            
            // 科目名・教室名を取得
            String subjectName = subjectRepository.findById(tt.getSubjectId())
                                .map(s -> s.getSubjectName()).orElse("科目ID:" + tt.getSubjectId());
            
            String classroomName = classroomRepository.findById(tt.getClassroomId())
                                .map(c -> c.getClassroomName()).orElse("教室ID:" + tt.getClassroomId());

            // 出席状態を取得
            String statusSymbol = ""; 
            
            Optional<AttendanceEntity> attOpt = attendanceRepository.findByStudentAndDateRange(student, date, date)
                    .stream()
                    .filter(a -> a.getTimeTable().getTimeTableId().equals(tt.getTimeTableId()))
                    .findFirst();

            // 出席状態に応じた記号を設定
            if (attOpt.isPresent()) {
                String statusName = attOpt.get().getStatus().getStatusName();
                if ("出席".equals(statusName)) statusSymbol = "〇";
                else if ("欠席".equals(statusName)) statusSymbol = "×";
                else if ("遅刻".equals(statusName)) statusSymbol = "△";
                else statusSymbol = statusName; 

            }

            // DTOに追加 (コンストラクタの第一引数に subjectId を追加)
            result.add(new DailyAttendanceDto(
                tt.getSubjectId(),
                tt.getSlotId(),
                subjectName,
                classroomName,
                statusSymbol
            ));
        }
        
        return result;
    }
}