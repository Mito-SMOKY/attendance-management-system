package com.example.attendancemanagementsystem.user.admin.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.TimeSlotRepository;
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
    @Autowired private TimeSlotRepository timeSlotRepository;

    public Map<Integer, String> getDepartmentOptions() {
        List<DepartmentEntity> allDepts = departmentRepository.findAll();
        Map<Integer, String> options = new LinkedHashMap<>();
        for (DepartmentEntity dept : allDepts) {
            List<EnrollmentsEntity> enList = enrollmentsRepository.findByDepartment_DepartmentId(dept.getDepartmentId());
            String gradeStr = (!enList.isEmpty()) ? enList.get(0).getGrade() + "年" : "(学年不明)";
            
            String courseName = "コース不明";
            String majorName = "学科不明";
            if (dept.getMajor() != null) {
                majorName = dept.getMajor().getMajorName();
                if (dept.getMajor().getCourse() != null) {
                    courseName = dept.getMajor().getCourse().getCourseName();
                }
            }
            String label = String.format("【%s】%s / %s / %s", courseName, majorName, gradeStr, dept.getClassName());
            options.put(dept.getDepartmentId(), label);
        }
        return options;
    }

    public List<UsersEntity> getTeacherList() {
        return usersRepository.findByUserTypeId(2);
    }

    /**
     * 週間スケジュール一括登録（修正版：上書き対応）
     */
    @Transactional
    public void registerWeeklySchedule(MdTimetableDto dto) {
        Integer deptId = dto.getDepartmentId();
        
        // 学科エンティティの取得
        DepartmentEntity department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("Department not found ID:" + deptId));

        List<TimetableEntity> entitiesToSave = new ArrayList<>();
        LocalDate current = dto.getStartDate();
        LocalDate end = dto.getEndDate();
        
        // DTOの構造はそのまま使用
        // Map<SlotID, Map<DayOfWeek, Cell>>
        Map<Integer, Map<String, Cell>> scheduleMap = dto.getScheduleMap();

        if (scheduleMap == null || scheduleMap.isEmpty()) return;

        List<TimeSlotEntity> allSlots = timeSlotRepository.findAllByOrderBySlotIdAsc();

        // 日付ループ
        while (!current.isAfter(end)) {
            String dayOfWeekKey = current.getDayOfWeek().name();
            
            // 時限ループ
            for (TimeSlotEntity ts : allSlots) {
                Integer slot = ts.getSlotId();

                if (scheduleMap.containsKey(slot)) {
                    Map<String, Cell> dayMap = scheduleMap.get(slot);
                    
                    if (dayMap != null && dayMap.containsKey(dayOfWeekKey)) {
                        Cell cell = dayMap.get(dayOfWeekKey);
                        
                        // 入力がある場合のみ処理
                        if (cell != null && cell.getSubjectId() != null) {
                            
                            // ★修正ポイント: いきなり new せず、まずは既存データを探す！
                            Optional<TimetableEntity> existingOpt = timetableRepository
                                .findByDepartment_DepartmentIdAndDateAndSlotId(deptId, current, slot);

                            TimetableEntity entity;

                            if (existingOpt.isPresent()) {
                                // --- パターンA: 既にあるなら、それを使う (UPDATEになる) ---
                                entity = existingOpt.get();
                            } else {
                                // --- パターンB: ないなら、新しく作る (INSERTになる) ---
                                entity = new TimetableEntity();
                                // キーとなる情報は新規のときだけセットすればOK（既存データは既に持ってるから）
                                entity.setDate(current);
                                entity.setSlotId(slot);
                                entity.setDepartment(department);
                            }

                            // --- 共通: 値のセット（上書き） ---
                            // ここは新規でも更新でも毎回セットする
                            
                            if (dto.getYear() != null) {
                                entity.setAcademicYear(dto.getYear());
                            } else {
                                // 指定がなければ日付から判定、あるいは既存の値を維持するなら if(entity.getAcademicYear() == null) 等の工夫も可
                                entity.setAcademicYear(current.getYear());
                            }
                            
                            // Cellから値を取り出してセット
                            entity.setSubjectId(cell.getSubjectId());
                            entity.setClassroomId(cell.getClassroomId());
                            entity.setUserId(cell.getUserId());

                            // 保存リストに追加
                            entitiesToSave.add(entity);
                        }
                    }
                }
            }
            current = current.plusDays(1);
        }

        // まとめて保存 (新規はINSERT, 既存はUPDATEが自動で発行されます)
        if (!entitiesToSave.isEmpty()) {
            timetableRepository.saveAll(entitiesToSave);
        }
    }

    /**
     * 1日分のデータを取得（日別編集用）
     */
    public MdTimetableDto getDailySchedule(Integer deptId, LocalDate date) {
        MdTimetableDto dto = new MdTimetableDto();
        dto.setDepartmentId(deptId);
        dto.setStartDate(date);
        
        List<TimetableEntity> entities = timetableRepository.findByDepartment_DepartmentIdAndDateOrderBySlotIdAsc(deptId, date);
        String dayOfWeek = date.getDayOfWeek().name();
        
        for (TimetableEntity entity : entities) {
            Integer slot = entity.getSlotId();
            Map<String, Cell> dayMap = dto.getScheduleMap().get(slot);
            Cell cell = dayMap.get(dayOfWeek);
            
            if (cell == null) {
                cell = new Cell();
                dayMap.put(dayOfWeek, cell);
            }
            
            cell.setSubjectId(entity.getSubjectId());
            cell.setClassroomId(entity.getClassroomId());
            cell.setUserId(entity.getUserId());
        }
        return dto;
    }

    /**
     * 1日分のデータを更新
     * ★修正ポイント: delete後に flush() を実行
     */
    @Transactional
    public void updateDailySchedule(MdTimetableDto dto) {
        Integer deptId = dto.getDepartmentId();
        LocalDate date = dto.getStartDate();
        DepartmentEntity department = departmentRepository.findById(deptId).orElseThrow();

        // 1. 既存データを削除
        timetableRepository.deleteByDepartment_DepartmentIdAndDate(deptId, date);
        
        // ★ここが重要！削除をDBに即時反映させる
        timetableRepository.flush();
        
        // 2. 新しいデータを登録
        List<TimetableEntity> entitiesToSave = new ArrayList<>();
        String dayOfWeekKey = date.getDayOfWeek().name();
        Map<Integer, Map<String, Cell>> scheduleMap = dto.getScheduleMap();
        
        for (Integer slot : scheduleMap.keySet()) {
            Map<String, Cell> dayMap = scheduleMap.get(slot);
            if (dayMap != null && dayMap.containsKey(dayOfWeekKey)) {
                Cell cell = dayMap.get(dayOfWeekKey);
                if (cell != null && cell.getSubjectId() != null) {
                    TimetableEntity entity = new TimetableEntity();
                    entity.setDate(date);
                    entity.setSlotId(slot);
                    entity.setDepartment(department);
                    entity.setAcademicYear(date.getYear());
                    entity.setSubjectId(cell.getSubjectId());
                    entity.setClassroomId(cell.getClassroomId());
                    entity.setUserId(cell.getUserId());
                    entitiesToSave.add(entity);
                }
            }
        }
        if (!entitiesToSave.isEmpty()) {
            timetableRepository.saveAll(entitiesToSave);
        }
    }

    /**
     * 参照モード用
     */
    public MdTimetableDto getWeeklyScheduleView(Integer deptId, LocalDate date) {
        MdTimetableDto dto = new MdTimetableDto();
        dto.setDepartmentId(deptId);

        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = monday.plusDays(4);

        dto.setStartDate(monday);
        dto.setEndDate(friday);

        List<TimetableEntity> entities = timetableRepository.findForWeeklyView(deptId, monday, friday);
        System.out.println("★Service: 検索結果 " + entities.size() + "件 (" + monday + " ～ " + friday + ")");

        for (TimetableEntity entity : entities) {
            String dayOfWeek = entity.getDate().getDayOfWeek().name();
            Integer slot = entity.getSlotId();
            
            Map<String, Cell> dayMap = dto.getScheduleMap().get(slot);
            Cell cell = dayMap.get(dayOfWeek);
            
            if (cell == null) {
                cell = new Cell();
                dayMap.put(dayOfWeek, cell);
            }
            
            cell.setSubjectId(entity.getSubjectId());
            if (entity.getSubject() != null) cell.setSubjectName(entity.getSubject().getSubjectName()); else cell.setSubjectName("不明(ID:" + entity.getSubjectId() + ")");
            
            cell.setClassroomId(entity.getClassroomId());
            if (entity.getClassroom() != null) cell.setClassroomName(entity.getClassroom().getClassroomName());
            
            cell.setUserId(entity.getUserId());
            if (entity.getUser() != null) cell.setTeacherName(entity.getUser().getName());
        }
        return dto;
    }
}