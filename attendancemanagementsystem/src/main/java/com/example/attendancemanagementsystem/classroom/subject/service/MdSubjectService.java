package com.example.attendancemanagementsystem.classroom.subject.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.classroom.subject.dto.SubjectMatrixRowDTO;
import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.DepartmentSubject;
import com.example.attendancemanagementsystem.common.entity.DepartmentSubjectKey;
import com.example.attendancemanagementsystem.common.entity.MajorEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectFaculty;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.common.repository.MajorRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectFacultyRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
public class MdSubjectService {

    @Autowired private SubjectRepository subjectRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private DepartmentSubjectRepository departmentSubjectRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private SubjectFacultyRepository subjectFacultyRepository;
    @Autowired private MajorRepository majorRepository;

    public List<SubjectMatrixRowDTO> getSubjectInfoList() {
        List<SubjectEntity> subjects = subjectRepository.findAll();
        List<DepartmentSubject> allRelations = departmentSubjectRepository.findAll();
        
        return subjects.stream().map(sub -> {
            SubjectMatrixRowDTO dto = new SubjectMatrixRowDTO();
            dto.setSubjectId(sub.getSubjectId());
            dto.setSubjectName(sub.getSubjectName());
            
            allRelations.stream()
                .filter(r -> r.getId().getSubjectId().equals(sub.getSubjectId()))
                .findFirst().ifPresent(rel -> {
                    dto.setGrade(rel.getGrade());
                    if (rel.getDepartment() != null) {
                        dto.setDepartmentId(rel.getDepartment().getDepartmentId());
                        dto.setClassName(rel.getDepartment().getClassName());
                        if (rel.getDepartment().getMajor() != null) {
                            dto.setMajorId(rel.getDepartment().getMajor().getMajorId());
                            dto.setMajorName(rel.getDepartment().getMajor().getMajorName());
                        }
                    }
                });

            List<SubjectFaculty> faculties = subjectFacultyRepository.findBySubjectId(sub.getSubjectId());
            List<Integer> tIds = new ArrayList<>();
            List<String> tNames = new ArrayList<>();
            for (SubjectFaculty sf : faculties) {
                tIds.add(sf.getId().getUserId());
                usersRepository.findById(sf.getId().getUserId()).ifPresent(u -> tNames.add(u.getName()));
            }
            dto.setTeacherIds(tIds);
            dto.setTeacherNames(tNames);
            
            return dto;
        }).collect(Collectors.toList());
    }

    public List<MajorEntity> getAllMajors() {
        return majorRepository.findAll();
    }

    public List<DepartmentEntity> getAllDepartments() {
        return departmentRepository.findAll();
    }

    // --- 保存・削除処理 ---
    @Transactional
    public List<String> saveSubjectList(
            List<Integer> subjectIds, 
            List<String> subjectNames, 
            List<Integer> teacherIds, 
            List<Integer> majorIds,        
            List<Integer> departmentIds,
            List<Integer> grades) {
    
        System.out.println("=== saveSubjectList 開始 ===");
        List<String> skippedSubjects = new ArrayList<>();

        if (subjectNames == null) return skippedSubjects;

        // --- 1. 重複＆必須チェック ---
        Map<String, Integer> processingKeys = new HashMap<>();

        for (int i = 0; i < subjectNames.size(); i++) {
            Integer deptId = departmentIds.get(i);
            Integer grade = grades.get(i);
            String name = subjectNames.get(i);
            Integer inputSubId = (subjectIds != null && subjectIds.size() > i) ? subjectIds.get(i) : null;

            // ★必須チェック: 項目が空ならエラーにする
            if (deptId == null || grade == null || name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("入力エラー: 必須項目（コース、クラス、学年、教科名）が入力されていない行があります。");
            }

            String key = deptId + "-" + grade + "-" + name.trim();
            System.out.printf("[Check] Dept:%d Grade:%d Name:%s (InputID:%s)%n", deptId, grade, name, inputSubId);

            // A. 画面内での重複チェック
            if (processingKeys.containsKey(key)) {
                Integer existingId = processingKeys.get(key);
                // IDが異なる(または片方が新規)なら、別行として重複している
                if (!Objects.equals(inputSubId, existingId)) {
                    throw new IllegalArgumentException("入力エラー: 教科「" + name + "」が重複して入力されています。");
                }
            } else {
                processingKeys.put(key, inputSubId);
            }

            // B. 教科マスタ全体での重複チェック (Subjectテーブル全体をチェック)
            Optional<SubjectEntity> existingSubject = subjectRepository.findBySubjectName(name.trim());
            
            if (existingSubject.isPresent()) {
                Integer existingId = existingSubject.get().getSubjectId();
                
                // 新規登録(null)しようとしたが、マスタに既に同じ名前がある -> エラー
                if (inputSubId == null) {
                    throw new IllegalArgumentException("登録エラー: 「" + name + "」は既に教科マスタに存在します。(ID: " + existingId + ")");
                }
                
                // 編集しようとしたが、別の教科IDと名前が被った -> エラー
                if (!inputSubId.equals(existingId)) {
                    throw new IllegalArgumentException("エラー: 「" + name + "」は既に教科マスタに存在するため、その名前に変更できません。");
                }
            }
        }

        // --- 2. 削除処理 ---
        List<Integer> activeSubjectIds = new ArrayList<>();
        if (subjectIds != null) {
            activeSubjectIds = subjectIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        List<SubjectEntity> allSubjects = subjectRepository.findAll();
        List<DepartmentSubject> allRelations = departmentSubjectRepository.findAll();

        for (SubjectEntity sub : allSubjects) {
            if (!activeSubjectIds.contains(sub.getSubjectId())) {
                int usageCount = subjectFacultyRepository.countTimeTableUsage(sub.getSubjectId());
                if (usageCount > 0) {
                    System.out.println("削除スキップ(使用中): " + sub.getSubjectName());
                    skippedSubjects.add(sub.getSubjectName());
                    continue; 
                }
                subjectFacultyRepository.deleteBySubjectId(sub.getSubjectId());
                allRelations.stream()
                    .filter(ds -> ds.getSubject().getSubjectId().equals(sub.getSubjectId()))
                    .forEach(ds -> departmentSubjectRepository.delete(ds));
                subjectRepository.delete(sub);
            }
        }

        // --- 3. 保存・更新処理 ---
        if (subjectNames != null && !subjectNames.isEmpty()) {
            Map<String, List<Integer>> groupTeacherMap = new LinkedHashMap<>();
            // Key: "Dept-Grade-Name", Value: SubjectID
            Map<String, Integer> groupSubjectIdMap = new HashMap<>();

            for (int i = 0; i < subjectNames.size(); i++) {
                Integer deptId = departmentIds.get(i);
                Integer grade = grades.get(i);
                String name = subjectNames.get(i);
                Integer tId = teacherIds.get(i);
                Integer sId = (subjectIds != null && subjectIds.size() > i) ? subjectIds.get(i) : null;

                if (deptId == null || grade == null || name == null) continue;

                String key = deptId + "-" + grade + "-" + name.trim();
                groupTeacherMap.computeIfAbsent(key, k -> new ArrayList<>()).add(tId);
                
                if (sId != null) {
                    groupSubjectIdMap.put(key, sId);
                }
            }

            for (Map.Entry<String, List<Integer>> entry : groupTeacherMap.entrySet()) {
                String key = entry.getKey();
                String[] parts = key.split("-");
                Integer dId = Integer.parseInt(parts[0]);
                Integer grade = Integer.parseInt(parts[1]);
                String sName = parts[2];
                
                List<Integer> tIds = entry.getValue();
                Integer currentSubjectId = groupSubjectIdMap.get(key);

                SubjectEntity subject;

                if (currentSubjectId != null) {
                    // IDあり: 更新
                    subject = subjectRepository.findById(currentSubjectId).orElse(new SubjectEntity());
                    subject.setSubjectName(sName);
                    subject = subjectRepository.save(subject);
                    
                    updateDepartmentSubject(dId, grade, subject);

                } else {
                    // IDなし: 新規作成
                    // 重複チェックを通過しているので、ここでは純粋な新規作成を行う
                    subject = new SubjectEntity();
                    subject.setSubjectName(sName);
                    subject.setRequiredCredits(1); 
                    subject.setTotalCredits(1);
                    subject = subjectRepository.save(subject);

                    updateDepartmentSubject(dId, grade, subject);
                }

                // 教員紐づけ更新
                subjectFacultyRepository.deleteBySubjectId(subject.getSubjectId());
                List<Integer> uniqueTeacherIds = tIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

                for (Integer tId : uniqueTeacherIds) {
                    subjectFacultyRepository.insertSubjectFaculty(subject.getSubjectId(), tId);
                }
            }
        }
        
        System.out.println("=== saveSubjectList 終了 ===");
        return skippedSubjects;
    }

    private void updateDepartmentSubject(Integer deptId, Integer grade, SubjectEntity subject) {
        // 既存の紐づけをクリーンアップして再登録
        List<DepartmentSubject> existing = departmentSubjectRepository.findAll();
        existing.stream()
            .filter(ds -> ds.getSubject().getSubjectId().equals(subject.getSubjectId()))
            .forEach(ds -> departmentSubjectRepository.delete(ds));
        
        DepartmentSubject ds = new DepartmentSubject();
        ds.setId(new DepartmentSubjectKey(deptId, subject.getSubjectId()));
        ds.setGrade(grade);
        departmentRepository.findById(deptId).ifPresent(ds::setDepartment);
        ds.setSubject(subject);
        departmentSubjectRepository.save(ds);
    }

    public List<Map<String, Object>> getSimpleMajorList() { return majorRepository.findAll().stream().map(m -> { Map<String, Object> map = new HashMap<>(); map.put("majorId", m.getMajorId()); map.put("majorName", m.getMajorName()); return map; }).collect(Collectors.toList()); }
    public List<Map<String, Object>> getSimpleDepartmentList() { return departmentRepository.findAll().stream().map(d -> { Map<String, Object> map = new HashMap<>(); map.put("departmentId", d.getDepartmentId()); map.put("className", d.getClassName()); return map; }).collect(Collectors.toList()); }
    public List<Map<String, Object>> getSimpleTeacherList() { return usersRepository.findByUserTypeId(2).stream().map(u -> { Map<String, Object> map = new HashMap<>(); map.put("teacherId", u.getUserId()); map.put("teacherName", u.getName()); return map; }).collect(Collectors.toList()); }
}