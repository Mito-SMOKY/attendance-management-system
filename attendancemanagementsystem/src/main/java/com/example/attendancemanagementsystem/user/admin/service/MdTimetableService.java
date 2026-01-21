package com.example.attendancemanagementsystem.user.admin.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.dto.MdTimetableDto;
import com.example.attendancemanagementsystem.user.admin.dto.MdTimetableDto.Cell;

@Service
public class MdTimetableService {

    @Autowired private TimetableRepository timetableRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private EnrollmentsRepository enrollmentsRepository;
    @Autowired private UsersRepository usersRepository;

    /**
     * 学科(クラス)選択用のドロップダウンリストを生成
     * Map<DepartmentID, 表示用ラベル>
     */
    public Map<Integer, String> getDepartmentOptions() {
        List<DepartmentEntity> allDepts = departmentRepository.findAll();
        Map<Integer, String> options = new LinkedHashMap<>();

        for (DepartmentEntity dept : allDepts) {
            // 学年の特定
            EnrollmentsEntity enrollment = enrollmentsRepository.findFirstByDepartmentAndIsActiveTrue(dept);
            String gradeStr = (enrollment != null) ? enrollment.getGrade() + "年" : "(学年不明)";
            
            // コース名・学科名の取得
            String courseName = "コース不明";
            String majorName = "学科不明";
            if (dept.getMajor() != null) {
                majorName = dept.getMajor().getMajorName();
                if (dept.getMajor().getCourse() != null) {
                    courseName = dept.getMajor().getCourse().getCourseName();
                }
            }

            // 表示ラベル: 【コース名】 学科 / 学年 / クラス
            String label = String.format("【%s】%s / %s / %s", 
                courseName, majorName, gradeStr, dept.getClassName());

            options.put(dept.getDepartmentId(), label);
        }
        return options;
    }

    /**
     * 教員（管理者）のみのリストを取得する
     */
    public List<UsersEntity> getTeacherList() {
        // DBに合わせて UserTypeID = 2 (administrator) を取得
        // これで生徒(ID=1)が表示されなくなります
        return usersRepository.findByUserTypeId(2);
    }

    /**
     * 週間スケジュールの一括登録
     */
    @Transactional
    public void registerWeeklySchedule(MdTimetableDto dto) {
        Integer deptId = dto.getDepartmentId();
        DepartmentEntity department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("Department not found ID:" + deptId));

        List<TimetableEntity> entitiesToSave = new ArrayList<>();
        LocalDate current = dto.getStartDate();
        LocalDate end = dto.getEndDate();
        Map<Integer, Map<String, Cell>> scheduleMap = dto.getScheduleMap();

        if (scheduleMap == null || scheduleMap.isEmpty()) return;

        while (!current.isAfter(end)) {
            String dayOfWeekKey = current.getDayOfWeek().name();
            // 1限～4限
            for (Integer slot = 1; slot <= 4; slot++) {
                if (scheduleMap.containsKey(slot)) {
                    Map<String, Cell> dayMap = scheduleMap.get(slot);
                    if (dayMap != null && dayMap.containsKey(dayOfWeekKey)) {
                        Cell cell = dayMap.get(dayOfWeekKey);
                        if (cell.getSubjectId() != null) {
                            TimetableEntity entity = new TimetableEntity();
                            entity.setDate(current);
                            entity.setSlotId(slot);
                            entity.setDepartment(department);
                            entity.setAcademicYear(current.getYear());
                            entity.setSubjectId(cell.getSubjectId());
                            entity.setClassroomId(cell.getClassroomId());
                            entity.setUserId(cell.getUserId());
                            entitiesToSave.add(entity);
                        }
                    }
                }
            }
            current = current.plusDays(1);
        }

        if (!entitiesToSave.isEmpty()) {
            timetableRepository.saveAll(entitiesToSave);
        }
    }
}