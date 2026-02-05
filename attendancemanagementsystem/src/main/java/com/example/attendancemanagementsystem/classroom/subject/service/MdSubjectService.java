package com.example.attendancemanagementsystem.classroom.subject.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    
        List<String> skippedSubjects = new ArrayList<>();

        if (subjectNames == null) return skippedSubjects;

        // --- ★追加: 重複チェック処理 ---
        // 画面内での重複、およびDBとの重複をチェックして、問題があれば例外を投げます。
        
        // キー: "DeptId-Grade-SubjectName", 値: SubjectId
        Map<String, Integer> processingKeys = new HashMap<>();
        List<DepartmentSubject> existingDBRelations = departmentSubjectRepository.findAll();

        for (int i = 0; i < subjectNames.size(); i++) {
            Integer deptId = departmentIds.get(i);
            Integer grade = grades.get(i);
            String name = subjectNames.get(i);
            Integer inputSubId = subjectIds.get(i); // 新規の場合はnull

            // 入力不備のスキップ（空行など）
            if (deptId == null || grade == null || name == null || name.trim().isEmpty()) {
                continue;
            }

            String key = deptId + "-" + grade + "-" + name.trim();

            // 1. 画面内で同じ設定の行が複数あるかチェック
            if (processingKeys.containsKey(key)) {
                Integer existingIdInBatch = processingKeys.get(key);
                // 既に処理リストにあるIDと異なる（あるいは片方がnull）場合、別行での重複登録とみなす
                // ※同じSubjectIDであれば、それは「同じ教科に複数の教員」のデータなのでOK
                if (!Objects.equals(inputSubId, existingIdInBatch)) {
                    throw new IllegalArgumentException("入力データに重複があります: " + name + " (同じクラス・学年に同名の教科を複数作成しようとしています)");
                }
            } else {
                processingKeys.put(key, inputSubId);
            }

            // 2. データベースに既に存在するかチェック
            DepartmentSubject duplicateTarget = existingDBRelations.stream()
                .filter(ds -> ds.getDepartment().getDepartmentId().equals(deptId) &&
                              ds.getGrade().equals(grade) &&
                              ds.getSubject().getSubjectName().equals(name.trim()))
                .findFirst()
                .orElse(null);

            if (duplicateTarget != null) {
                Integer dbSubjectId = duplicateTarget.getSubject().getSubjectId();
                // DBにあるのに、入力データのIDが null (新規作成) または 違うID (別教科をリネームして衝突) の場合
                if (inputSubId == null || !inputSubId.equals(dbSubjectId)) {
                    throw new IllegalArgumentException("既に登録済みの教科です: " + name + " (このクラス・学年には既に存在します)");
                }
            }
        }
        // ---------------------------------


        // 1. 削除処理
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
                
                // 時間割で使用されているかチェック
                int usageCount = subjectFacultyRepository.countTimeTableUsage(sub.getSubjectId());
                if (usageCount > 0) {
                    skippedSubjects.add(sub.getSubjectName());
                    continue; 
                }

                // 削除実行
                subjectFacultyRepository.deleteBySubjectId(sub.getSubjectId());
                
                allRelations.stream()
                    .filter(ds -> ds.getSubject().getSubjectId().equals(sub.getSubjectId()))
                    .forEach(ds -> departmentSubjectRepository.delete(ds));

                subjectRepository.delete(sub);
            }
        }

        // 2. 保存・更新処理
        if (subjectNames != null && !subjectNames.isEmpty()) {
            Map<String, List<Integer>> groupMap = new LinkedHashMap<>();
            for (int i = 0; i < subjectNames.size(); i++) {
                String key = String.format("%s-%s-%s-%s", majorIds.get(i), departmentIds.get(i), grades.get(i), subjectNames.get(i));
                groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(teacherIds.get(i));
            }

            for (Map.Entry<String, List<Integer>> entry : groupMap.entrySet()) {
                String[] parts = entry.getKey().split("-");
                Integer dId = Integer.parseInt(parts[1]);
                Integer grade = Integer.parseInt(parts[2]);
                String sName = parts[3];
                List<Integer> tIds = entry.getValue();

                SubjectEntity subject = findOrCreateSubject(dId, grade, sName);
                
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
        
        return skippedSubjects;
    }

    public List<Map<String, Object>> getSimpleMajorList() {
        return majorRepository.findAll().stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("majorId", m.getMajorId());
            map.put("majorName", m.getMajorName());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getSimpleDepartmentList() {
        return departmentRepository.findAll().stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("departmentId", d.getDepartmentId());
            map.put("className", d.getClassName());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getSimpleTeacherList() {
        return usersRepository.findByUserTypeId(2).stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("teacherId", u.getUserId());
            map.put("teacherName", u.getName());
            return map;
        }).collect(Collectors.toList());
    }

    private SubjectEntity findOrCreateSubject(Integer deptId, Integer grade, String name) {
        return departmentSubjectRepository.findAll().stream()
            .filter(ds -> ds.getGrade().equals(grade) && 
                          ds.getDepartment().getDepartmentId().equals(deptId) && 
                          ds.getSubject().getSubjectName().equals(name))
            .map(DepartmentSubject::getSubject)
            .findFirst()
            .orElseGet(() -> {
                SubjectEntity s = new SubjectEntity();
                s.setSubjectName(name);
                s.setRequiredCredits(1); 
                s.setTotalCredits(1);
                SubjectEntity saved = subjectRepository.save(s);

                DepartmentSubject ds = new DepartmentSubject();
                ds.setId(new DepartmentSubjectKey(deptId, saved.getSubjectId()));
                ds.setGrade(grade);
                departmentRepository.findById(deptId).ifPresent(ds::setDepartment);
                ds.setSubject(saved);
                departmentSubjectRepository.save(ds);
                return saved;
            });
    }
}