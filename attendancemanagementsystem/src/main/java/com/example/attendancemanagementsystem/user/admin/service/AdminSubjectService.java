package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.DepartmentSubject;
import com.example.attendancemanagementsystem.common.entity.DepartmentSubjectKey;
import com.example.attendancemanagementsystem.common.entity.MajorEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.common.repository.MajorRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectFacultyRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.dto.SubjectMatrixRowDTO;

@Service
public class AdminSubjectService {

    @Autowired private SubjectRepository subjectRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private DepartmentSubjectRepository departmentSubjectRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private SubjectFacultyRepository subjectFacultyRepository;
    @Autowired private MajorRepository majorRepository;

    // --- 教科一覧取得（情報マスタ画面用） ---
    public List<SubjectMatrixRowDTO> getSubjectInfoList() {
        List<SubjectEntity> subjects = subjectRepository.findAll();
        // 紐づけ情報を全て取得
        List<DepartmentSubject> allRelations = departmentSubjectRepository.findAll();
        
        return subjects.stream().map(sub -> {
            SubjectMatrixRowDTO dto = new SubjectMatrixRowDTO();
            dto.setSubjectId(sub.getSubjectId());
            dto.setSubjectName(sub.getSubjectName());
            dto.setCourseCount(sub.getRequiredCredits() != null ? sub.getRequiredCredits() : 1);
            
            // 教科IDから紐づけ情報を探して、学年・コース・クラスをセット
            DepartmentSubject rel = allRelations.stream()
                .filter(r -> r.getId().getSubjectId().equals(sub.getSubjectId()))
                .findFirst()
                .orElse(null);

            if (rel != null) {
                dto.setGrade(rel.getGrade());
                if (rel.getDepartment() != null) {
                    dto.setClassName(rel.getDepartment().getClassName());
                    if (rel.getDepartment().getMajor() != null) {
                        dto.setMajorName(rel.getDepartment().getMajor().getMajorName());
                    }
                }
            } else {
                // 紐づけがない場合
                dto.setGrade(null);
                dto.setClassName("-");
                dto.setMajorName("-");
            }

            // 教師情報のセット
            Integer teacherId = subjectFacultyRepository.findTeacherIdBySubjectId(sub.getSubjectId());
            if (teacherId != null) {
                dto.setTeacherId(teacherId);
                usersRepository.findById(teacherId).ifPresent(user -> {
                    dto.setTeacherName(user.getName());
                });
            } else {
                dto.setTeacherId(null);
                dto.setTeacherName("未設定");
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // --- 教師リスト取得 ---
    public List<SubjectMatrixRowDTO> getTeacherList() {
        List<UsersEntity> users = usersRepository.findByUserTypeId(2);
        return users.stream().map(user -> {
            SubjectMatrixRowDTO dto = new SubjectMatrixRowDTO();
            dto.setTeacherId(user.getUserId());
            dto.setTeacherName(user.getName());
            return dto;
        }).collect(Collectors.toList());
    }

    // --- コース一覧取得 ---
    public List<MajorEntity> getAllMajors() {
        return majorRepository.findAll();
    }

    // --- 全クラス一覧取得 ---
    public List<DepartmentEntity> getAllDepartments() {
        return departmentRepository.findAll();
    }

    // --- マトリクスデータ取得 ---
    public List<SubjectMatrixRowDTO> getSubjectMatrixData() {
        List<SubjectEntity> subjects = subjectRepository.findAll();
        List<DepartmentSubject> relations = departmentSubjectRepository.findAll();
        List<DepartmentEntity> departments = departmentRepository.findAll();

        List<SubjectMatrixRowDTO> matrix = new ArrayList<>();

        for (SubjectEntity sub : subjects) {
            SubjectMatrixRowDTO dto = new SubjectMatrixRowDTO();
            dto.setSubjectId(sub.getSubjectId());
            dto.setSubjectName(sub.getSubjectName());
            
            DepartmentSubject firstRel = relations.stream()
                .filter(r -> r.getId().getSubjectId().equals(sub.getSubjectId()))
                .findFirst().orElse(null);
            
            if(firstRel != null) {
                dto.setGrade(firstRel.getGrade());
            } else {
                dto.setGrade(1);
            }

            Map<String, Boolean> statusMap = new HashMap<>();
            for (DepartmentEntity dept : departments) {
                boolean isRelated = relations.stream().anyMatch(rel -> 
                    rel.getId().getSubjectId().equals(sub.getSubjectId()) && 
                    rel.getId().getDepartmentId().equals(dept.getDepartmentId())
                );
                statusMap.put(String.valueOf(dept.getDepartmentId()), isRelated);
            }
            dto.setStatusMap(statusMap);
            matrix.add(dto);
        }
        return matrix;
    }

    // --- 新しい教科登録処理（個別登録用） ---
    @Transactional
    public void createAndLinkSubject(
            Integer grade, 
            Integer majorId, 
            Integer departmentId, 
            String rawSubjectName, 
            Integer teacherId) {
        
        DepartmentEntity dept = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new RuntimeException("指定されたクラスが見つかりません"));
        
        String majorName = dept.getMajor().getMajorName();
        String className = dept.getClassName();
        String gradeStr = grade + "年";

        String fullName = String.format("%s %s %s %s", gradeStr, majorName, className, rawSubjectName);

        SubjectEntity subject = new SubjectEntity();
        subject.setSubjectName(fullName);
        subject.setRequiredCredits(1);
        subject.setTotalCredits(1);
        
        SubjectEntity savedSubject = subjectRepository.save(subject);

        DepartmentSubject link = new DepartmentSubject();
        DepartmentSubjectKey key = new DepartmentSubjectKey(departmentId, savedSubject.getSubjectId());
        
        link.setId(key);
        link.setDepartment(dept);
        link.setSubject(savedSubject);
        link.setGrade(grade);

        departmentSubjectRepository.save(link);
        
        if (teacherId != null) {
             subjectFacultyRepository.insertSubjectFaculty(savedSubject.getSubjectId(), teacherId);
        }
    }

    // --- 保存処理（情報マスタ画面用：一括保存） ---
    @Transactional
    public void saveSubjectList(
            List<Integer> subjectIds, 
            List<String> subjectNames, 
            List<Integer> teacherIds, 
            List<Integer> courseCounts,
            List<Integer> majorIds,        // ★この行を追加してください
            List<Integer> departmentIds,
            List<Integer> grades) {
    
        if (subjectNames == null) return;

        // 1. 削除された行の処理
        // 画面に残っているIDリストに含まれない既存の教科を削除します
        List<Integer> keptIds = new ArrayList<>();
        if (subjectIds != null) {
            for(Integer id : subjectIds) {
                if(id != null) keptIds.add(id);
            }
        }
        
        List<SubjectEntity> allSubjects = subjectRepository.findAll();
        for (SubjectEntity sub : allSubjects) {
            if (!keptIds.contains(sub.getSubjectId())) {
                // 関連テーブルの削除
                departmentSubjectRepository.deleteAll(
                    departmentSubjectRepository.findAll().stream()
                        .filter(ds -> ds.getId().getSubjectId().equals(sub.getSubjectId()))
                        .collect(Collectors.toList())
                );
                subjectFacultyRepository.deleteBySubjectId(sub.getSubjectId());
                subjectRepository.delete(sub); 
            }
        }

        // 2. 追加・更新の処理
        for (int i = 0; i < subjectNames.size(); i++) {
            String name = subjectNames.get(i);
            Integer currentId = (subjectIds != null && subjectIds.size() > i) ? subjectIds.get(i) : null;
            Integer teacherId = (teacherIds != null && teacherIds.size() > i) ? teacherIds.get(i) : null;
            Integer departmentId = (departmentIds != null && departmentIds.size() > i) ? departmentIds.get(i) : null;

            // ★修正箇所: コマ数と学年の取得ロジックを修正 (null安全にする)
            Integer countVal = (courseCounts != null && courseCounts.size() > i) ? courseCounts.get(i) : null;
            Integer count = (countVal != null) ? countVal : 1; // nullなら1にする

            Integer gradeVal = (grades != null && grades.size() > i) ? grades.get(i) : null;
            Integer grade = (gradeVal != null) ? gradeVal : 1; // nullなら1にする
            
            // --- 以下、元のコードと同じ ---
            // SubjectEntity (教科本体) の保存
            SubjectEntity entity;
            if (currentId != null) {
                entity = subjectRepository.findById(currentId).orElse(new SubjectEntity());
            } else {
                entity = new SubjectEntity();
            }

            entity.setSubjectName(name);
            entity.setRequiredCredits(count); 
            if (entity.getTotalCredits() == null) entity.setTotalCredits(count); 
            
            SubjectEntity savedEntity = subjectRepository.save(entity);
            Integer savedSubjectId = savedEntity.getSubjectId();

            // SubjectFaculty (担当教師) の保存
            subjectFacultyRepository.deleteBySubjectId(savedSubjectId);
            if (teacherId != null) {
                subjectFacultyRepository.insertSubjectFaculty(savedSubjectId, teacherId);
            }

            // DepartmentSubject (クラス・学年との紐付け) の保存
            List<DepartmentSubject> existingLinks = departmentSubjectRepository.findAll().stream()
                .filter(ds -> ds.getId().getSubjectId().equals(savedSubjectId))
                .collect(Collectors.toList());
            departmentSubjectRepository.deleteAll(existingLinks);

            if (departmentId != null) {
                DepartmentSubject newLink = new DepartmentSubject();
                DepartmentSubjectKey key = new DepartmentSubjectKey(departmentId, savedSubjectId);
                
                newLink.setId(key);
                newLink.setGrade(grade);
                
                departmentRepository.findById(departmentId).ifPresent(newLink::setDepartment);
                newLink.setSubject(savedEntity);

                departmentSubjectRepository.save(newLink);
            }
        }
    }

    // --- マトリクス保存処理 (既存のまま) ---
    @Transactional
    public void saveSubjectMatrix(List<String> activePairs) {
        if (activePairs == null) activePairs = new ArrayList<>();

        List<DepartmentSubject> currentRelations = departmentSubjectRepository.findAll();

        for (DepartmentSubject rel : currentRelations) {
            String key = rel.getId().getSubjectId() + "-" + rel.getId().getDepartmentId();
            if (!activePairs.contains(key)) {
                departmentSubjectRepository.delete(rel);
            }
        }

        List<String> currentKeys = currentRelations.stream()
                .map(rel -> rel.getId().getSubjectId() + "-" + rel.getId().getDepartmentId())
                .collect(Collectors.toList());

        for (String pair : activePairs) {
            if (!currentKeys.contains(pair)) {
                String[] parts = pair.split("-");
                Integer sId = Integer.parseInt(parts[0]);
                Integer dId = Integer.parseInt(parts[1]);

                DepartmentSubject ds = new DepartmentSubject();
                ds.setId(new DepartmentSubjectKey(dId, sId));
                ds.setGrade(1); 

                ds.setDepartment(departmentRepository.findById(dId).orElse(null));
                ds.setSubject(subjectRepository.findById(sId).orElse(null));

                if (ds.getDepartment() != null && ds.getSubject() != null) {
                    departmentSubjectRepository.save(ds);
                }
            }
        }
    }

    // ★追加: 循環参照を避けるために、IDと名前だけのMapリストを作る
    public List<Map<String, Object>> getSimpleMajorList() {
        return majorRepository.findAll().stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("majorId", m.getMajorId());
            map.put("majorName", m.getMajorName());
            return map;
        }).collect(Collectors.toList());
    }

    // ★追加
    public List<Map<String, Object>> getSimpleDepartmentList() {
        return departmentRepository.findAll().stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("departmentId", d.getDepartmentId());
            map.put("className", d.getClassName());
            // 関連するSubjectリストなどは入れない！これでループ回避
            return map;
        }).collect(Collectors.toList());
    }

    // ★追加
    public List<Map<String, Object>> getSimpleTeacherList() {
        return usersRepository.findByUserTypeId(2).stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("teacherId", u.getUserId());
            map.put("teacherName", u.getName());
            return map;
        }).collect(Collectors.toList());
    }

}