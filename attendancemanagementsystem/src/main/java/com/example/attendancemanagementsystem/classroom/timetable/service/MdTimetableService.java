package com.example.attendancemanagementsystem.classroom.timetable.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter; 
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.classroom.timetable.dto.MdTimetableDto;
import com.example.attendancemanagementsystem.classroom.timetable.dto.MdTimetableDto.Cell;
import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.TimeSlotRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
public class MdTimetableService {

    // 必要なリポジトリの注入
    @Autowired private TimetableRepository timetableRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private TimeSlotRepository timeSlotRepository;

    // 学科の選択肢リスト作成
    public Map<Integer, String> getDepartmentOptions() {
        // 全クラスデータを取得
        List<DepartmentEntity> deptList = departmentRepository.findAll();
        
        // 順序を保持するためのLinkedHashMap
        Map<Integer, String> options = new LinkedHashMap<>();
        
        for (DepartmentEntity dept : deptList) {
            StringBuilder label = new StringBuilder();

            // 学科名を取得
            if (dept.getMajor() != null) {
                label.append(dept.getMajor().getMajorName());
                
                // コース名を取得
                if (dept.getMajor().getCourse() != null) {
                    label.append(dept.getMajor().getCourse().getCourseName());
                }
            } else {
                label.append("学科不明");
            }

            // クラス名の取得
            if (dept.getClassName() != null && !dept.getClassName().isEmpty()) {
                label.append(" ").append(dept.getClassName());
            }

            // Mapに格納
            options.put(dept.getDepartmentId(), label.toString());
        }
        return options;
    }
    // 教員のリストを取得
    public List<UsersEntity> getTeacherList() {
        return usersRepository.findByUserTypeId(2);
    }

    // 週間スケジュールを一括で登録・更新する
    @Transactional
    public void registerWeeklySchedule(MdTimetableDto dto) {
        Integer deptId = dto.getDepartmentId();
        Integer targetGrade = dto.getTargetGrade(); 
        
        // 学科エンティティの取得
        DepartmentEntity department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("Department not found ID:" + deptId));
        
        // 学年が指定されていない場合のデフォルト処理 
        if (targetGrade == null) targetGrade = 1; 

        // 除外日リストの作成 
        Set<LocalDate> skipDates = new HashSet<>();
        String excludedStr = dto.getExcludedDates();

        // ログ: 受け取った文字列を確認
        System.out.println("【Debug】除外日文字列(Raw): " + excludedStr);

        if (excludedStr != null && !excludedStr.trim().isEmpty()) {
            
            // JSON形式の記号（ブラケットやクォート）を除去して綺麗にする
            String cleanStr = excludedStr.replaceAll("[\\[\\]\"']", "");

            // カンマ(半角・全角)、読点、改行、スペースなどで分割
            String[] dates = cleanStr.split("[,、\n\r\\s]+");
            
            // 対応するフォーマット定義 
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_LOCAL_DATE,       // 2025-04-29
                DateTimeFormatter.ofPattern("yyyy/MM/dd"), // 2025/04/29
                DateTimeFormatter.ofPattern("yyyy/M/d")    // 2025/4/29
            };

            for (String d : dates) {
                String cleanDate = d.trim();
                if (cleanDate.isEmpty()) continue;
                
                boolean parsed = false;
                for (DateTimeFormatter fmt : formatters) {
                    try {
                        skipDates.add(LocalDate.parse(cleanDate, fmt));
                        parsed = true;
                        break; 
                    } catch (Exception e) {
                    }
                }
                
                if (!parsed) {
                    System.err.println("【Warning】日付として解析できませんでした: " + cleanDate);
                }
            }
        }
        
        // ログ: 実際に除外される日付リスト
        System.out.println("【Debug】登録スキップ対象の日付: " + skipDates);

        List<TimetableEntity> entitiesToSave = new ArrayList<>();
        LocalDate current = dto.getStartDate();
        LocalDate end = dto.getEndDate();
        
        // 入力データのマップを取得
        Map<Integer, Map<String, Cell>> scheduleMap = dto.getScheduleMap();

        if (scheduleMap == null || scheduleMap.isEmpty()) return;

        List<TimeSlotEntity> allSlots = timeSlotRepository.findAllByOrderBySlotIdAsc();

        // 指定期間の日付ループ
        while (!current.isAfter(end)) {

            // 除外日に含まれる場合はスキップ
            if (skipDates.contains(current)) {
                System.out.println("Skip: " + current + " (除外日のためスキップ)");
                current = current.plusDays(1);
                continue;
            }

            String dayOfWeekKey = current.getDayOfWeek().name();
            
            // 全時限ループ
            for (TimeSlotEntity ts : allSlots) {
                Integer slot = ts.getSlotId();

                if (scheduleMap.containsKey(slot)) {
                    Map<String, Cell> dayMap = scheduleMap.get(slot);
                    
                    if (dayMap != null && dayMap.containsKey(dayOfWeekKey)) {
                        Cell cell = dayMap.get(dayOfWeekKey);
                        
                        // 入力があるコマのみ処理
                        if (cell != null && cell.getSubjectId() != null) {
                            
                            // 既存データの検索 
                            Optional<TimetableEntity> existingOpt = timetableRepository
                                .findByDepartment_DepartmentIdAndGradeAndDateAndSlotId(deptId, targetGrade, current, slot);

                            TimetableEntity entity;

                            if (existingOpt.isPresent()) {

                                // 既存データがあれば更新モード
                                entity = existingOpt.get();
                            } else {

                                // なければ新規作成モード
                                entity = new TimetableEntity();

                                // 新規作成時のみキー情報をセット
                                entity.setDate(current);
                                entity.setSlotId(slot);
                                entity.setDepartment(department);
                                entity.setGrade(targetGrade); 
                            }

                            // 共通項目のセット
                            if (dto.getYear() != null) {
                                entity.setAcademicYear(dto.getYear());
                            } else {
                                entity.setAcademicYear(current.getYear());
                            }
                            
                            // 画面からの入力値をエンティティに反映
                            entity.setSubjectId(cell.getSubjectId());
                            entity.setClassroomId(cell.getClassroomId());
                            entity.setUserId(cell.getUserId());

                            // 保存リストに追加
                            entitiesToSave.add(entity);
                        }
                    }
                }
            }

            // 次の日付へ
            current = current.plusDays(1);
        }

        // 変更があったデータを一括保存
        if (!entitiesToSave.isEmpty()) {
            timetableRepository.saveAll(entitiesToSave);
        }
    }

    // 指定した日付のスケジュールを取得し、整形 
    public MdTimetableDto getDailySchedule(Integer deptId, Integer targetGrade, LocalDate date) {
        MdTimetableDto dto = new MdTimetableDto();
        dto.setDepartmentId(deptId);
        dto.setTargetGrade(targetGrade);
        dto.setStartDate(date);
        
        if (targetGrade == null) targetGrade = 1;

        // 検索時に学年を指定
        List<TimetableEntity> entities = timetableRepository.findByDepartment_DepartmentIdAndGradeAndDateOrderBySlotIdAsc(deptId, targetGrade, date);
        
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

    // 1日分のスケジュールを更新
    @Transactional
    public void updateDailySchedule(MdTimetableDto dto) {
        Integer deptId = dto.getDepartmentId();
        LocalDate date = dto.getStartDate();
        Integer targetGrade = dto.getTargetGrade(); 
        
        DepartmentEntity department = departmentRepository.findById(deptId).orElseThrow();
        if (targetGrade == null) targetGrade = 1;

        // 既存データを取得してMapにする
        List<TimetableEntity> existingList = timetableRepository.findByDepartment_DepartmentIdAndGradeAndDateOrderBySlotIdAsc(deptId, targetGrade, date);
        Map<Integer, TimetableEntity> existingMap = new HashMap<>();
        for (TimetableEntity t : existingList) {
            existingMap.put(t.getSlotId(), t);
        }
        
        // 保存用・削除用リスト
        List<TimetableEntity> entitiesToSave = new ArrayList<>();
        List<TimetableEntity> entitiesToDelete = new ArrayList<>();
        String dayOfWeekKey = date.getDayOfWeek().name();
        Map<Integer, Map<String, Cell>> scheduleMap = dto.getScheduleMap();
        
        // 全時限ループ
        for (Integer slot : scheduleMap.keySet()) {
            
            // 入力データの取得
            Cell cell = null;
            Map<String, Cell> dayMap = scheduleMap.get(slot);
            if (dayMap != null) {
                if (dayMap.containsKey(dayOfWeekKey)) {
                    cell = dayMap.get(dayOfWeekKey);
                } else if (dayMap.containsKey(dayOfWeekKey.toLowerCase())) {
                    cell = dayMap.get(dayOfWeekKey.toLowerCase());
                }
            }

            //同じ時限・教員の重複を解消
            if (cell != null && cell.getUserId() != null) {

                // その先生の、その日の授業を全取得
                List<TimetableEntity> teacherDailySchedule = timetableRepository.findByUserIdAndDate(cell.getUserId(), date);
                
                for (TimetableEntity conflict : teacherDailySchedule) {

                    // 「同じ時限」かつ「違う学科」のデータがあれば削除対象にする
                    if (conflict.getSlotId().equals(slot) && !conflict.getDepartment().getDepartmentId().equals(deptId)) {
                        entitiesToDelete.add(conflict);
                    }
                }
            }

            // DB上の既存データ
            TimetableEntity currentEntity = existingMap.get(slot);

            // 入力あり
            if (cell != null && cell.getSubjectId() != null) {

                if (currentEntity != null) {

                    //上書き処理
                    currentEntity.setSubjectId(cell.getSubjectId());
                    currentEntity.setClassroomId(cell.getClassroomId());
                    currentEntity.setUserId(cell.getUserId());
                    entitiesToSave.add(currentEntity);
                    
                    existingMap.remove(slot);
                } else {

                    // 新規登録
                    TimetableEntity newEntity = new TimetableEntity();
                    newEntity.setDate(date);
                    newEntity.setSlotId(slot);
                    newEntity.setDepartment(department);
                    newEntity.setGrade(targetGrade);
                    newEntity.setAcademicYear(date.getYear());
                    newEntity.setSubjectId(cell.getSubjectId());
                    newEntity.setClassroomId(cell.getClassroomId());
                    newEntity.setUserId(cell.getUserId());
                    entitiesToSave.add(newEntity);
                }
            } 

            // 入力なし
            else {
                if (currentEntity != null) {
                    entitiesToDelete.add(currentEntity);
                    existingMap.remove(slot);
                }
            }
        }

        // 保存実行
        if (!entitiesToSave.isEmpty()) {
            timetableRepository.saveAll(entitiesToSave);
        }
        
        // 削除実行
        if (!entitiesToDelete.isEmpty()) {
            timetableRepository.deleteAll(entitiesToDelete);
        }
    }

    // 週間スケジュールを参照用に取得 
    public MdTimetableDto getWeeklyScheduleView(Integer deptId, Integer targetGrade, LocalDate date) {
        MdTimetableDto dto = new MdTimetableDto();
        dto.setDepartmentId(deptId);
        dto.setTargetGrade(targetGrade);

        // 週の開始日と終了日を計算
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = monday.plusDays(4);

        dto.setStartDate(monday);
        dto.setEndDate(friday);
        
        if (targetGrade == null) targetGrade = 1;

        // データ取得 
        List<TimetableEntity> entities = timetableRepository.findForWeeklyView(deptId, targetGrade, monday, friday);

        // 取得データを画面表示用DTOにマッピング
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

    // 期間指定で一括削除
    @Transactional
    public void deleteRangeSchedule(MdTimetableDto dto) {
        Integer deptId = dto.getDepartmentId();
        Integer targetGrade = dto.getTargetGrade();
        LocalDate start = dto.getStartDate();
        LocalDate end = dto.getEndDate();

        if (deptId == null || start == null || end == null) {
            throw new RuntimeException("削除に必要な情報が不足しています");
        }
        if (targetGrade == null) targetGrade = 1;

        // リポジトリの削除メソッドを呼ぶ
        timetableRepository.deleteByDepartmentAndGradeAndDateRange(deptId, targetGrade, start, end);
    }
}