package com.example.attendancemanagementsystem.classroom.timetable.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.classroom.timetable.dto.StudentTimetableDto;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.ClassroomRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
@Transactional(readOnly = true)
public class StudentTimetableService {

    private final UsersRepository usersRepository;
    private final EnrollmentsRepository enrollmentsRepository;
    private final TimetableRepository timetableRepository;
    private final SubjectRepository subjectRepository;
    private final ClassroomRepository classroomRepository;

    public StudentTimetableService(
            UsersRepository usersRepository,
            EnrollmentsRepository enrollmentsRepository,
            TimetableRepository timetableRepository,
            SubjectRepository subjectRepository,
            ClassroomRepository classroomRepository) {
        this.usersRepository = usersRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.timetableRepository = timetableRepository;
        this.subjectRepository = subjectRepository;
        this.classroomRepository = classroomRepository;
    }

    /**
     * 指定された日付を含む週の時間割データを取得する
     */
    public StudentTimetableDto getTimetableData(String loginId, LocalDate referenceDate) {
        StudentTimetableDto dto = new StudentTimetableDto();

        // 1. 基準日からその週の月曜日を算出
        LocalDate monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = monday.plusDays(4);

        dto.setWeekStart(monday.toString()); // YYYY-MM-DD

        // 2. 週番号の計算
        int weekNum = referenceDate.get(WeekFields.of(Locale.JAPAN).weekOfMonth());
        dto.setWeekNumber(weekNum);

        // 3. 日付リストの生成 (月～金)
        List<String> dates = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            dates.add(monday.plusDays(i).toString());
        }
        dto.setDates(dates);

        // 4. 生徒の所属学科を取得
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        EnrollmentsEntity enrollment = enrollmentsRepository.findByUserAndIsActiveTrue(user)
                .orElse(null);

        if (enrollment == null) {
            dto.setSchedule(new HashMap<>());
            return dto;
        }

        Integer deptId = enrollment.getDepartment().getDepartmentId();

        // 5. DBから時間割データを取得
        List<TimetableEntity> timetables = timetableRepository
                .findByDepartment_DepartmentIdAndDateBetweenOrderByDateAscSlotIdAsc(deptId, monday, friday);

        // 6. データをDTOのMap形式に変換
        Map<String, Map<Integer, StudentTimetableDto.ClassDetail>> scheduleMap = new HashMap<>();

        for (TimetableEntity tt : timetables) {
            String dateKey = tt.getDate().toString();
            
            // スロットID (1始まり) を インデックス (0始まり) に変換
            int slotIndex = tt.getSlotId() - 1; 

            String subjectName = subjectRepository.findById(tt.getSubjectId())
                    .map(s -> s.getSubjectName()).orElse("不明な科目");
            
            String classroomName = classroomRepository.findById(tt.getClassroomId())
                    .map(c -> c.getClassroomName()).orElse("-");

            scheduleMap.putIfAbsent(dateKey, new HashMap<>());
            scheduleMap.get(dateKey).put(slotIndex, new StudentTimetableDto.ClassDetail(subjectName, classroomName));
        }

        dto.setSchedule(scheduleMap);

        return dto;
    }
}