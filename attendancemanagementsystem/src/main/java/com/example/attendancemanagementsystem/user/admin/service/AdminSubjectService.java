package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.CourseEntity;
import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.DepartmentSubject;
import com.example.attendancemanagementsystem.common.entity.DepartmentSubjectKey;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.CourseRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectFacultyRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.dto.SubjectMatrixRowDTO;

@Service
public class AdminSubjectService {

    @Autowired private SubjectRepository subjectRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private DepartmentSubjectRepository departmentSubjectRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private SubjectFacultyRepository subjectFacultyRepository;
    @Autowired private CourseRepository courseRepository;
    
    // ★追加: 時間割削除のためにRepositoryを注入
    @Autowired private TimetableRepository timetableRepository; 

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


                    if (rel.getDepartment().getCourse() != null) {
                        dto.setCourseName(rel.getDepartment().getCourse().getCourseName());
                    }
                }
            } else {
                // 紐づけがない場合
                dto.setGrade(null);
                dto.setClassName("-");
                dto.setCourseName("-");
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
    public List<CourseEntity> getAllcourses() {
        return courseRepository.findAll();
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
        
        String courseName = dept.getCourse().getCourseName();
        String className = dept.getClassName();
        String gradeStr = grade + "年";

        String fullName = String.format("%s %s %s %s", gradeStr, courseName, className, rawSubjectName);
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
            List<Integer> courseIds,
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
                
                // (1) クラス紐づけ(DepartmentSubject)を削除
                departmentSubjectRepository.deleteAll(
                    departmentSubjectRepository.findAll().stream()
                        .filter(ds -> ds.getId().getSubjectId().equals(sub.getSubjectId()))
                        .collect(Collectors.toList())
                );
                
                // (2) 担当教師(SubjectFaculty)を削除
                subjectFacultyRepository.deleteBySubjectId(sub.getSubjectId());
                
                // ★追加: (3) 時間割(TimeTable)を削除 (外部キー制約エラー回避のため必須)
                timetableRepository.deleteBySubjectId(sub.getSubjectId());

                // (4) 最後に教科本体(Subject)を削除
                subjectRepository.delete(sub); 
            }
        }

        // 2. 追加・更新の処理
        for (int i = 0; i < subjectNames.size(); i++) {
            String name = subjectNames.get(i);
            Integer currentId = (subjectIds != null && subjectIds.size() > i) ? subjectIds.get(i) : null;
            Integer teacherId = (teacherIds != null && teacherIds.size() > i) ? teacherIds.get(i) : null;
            Integer departmentId = (departmentIds != null && departmentIds.size() > i) ? departmentIds.get(i) : null;

            Integer countVal = (courseCounts != null && courseCounts.size() > i) ? courseCounts.get(i) : null;
            Integer count = (countVal != null) ? countVal : 1; 

            Integer gradeVal = (grades != null && grades.size() > i) ? grades.get(i) : null;
            Integer grade = (gradeVal != null) ? gradeVal : 1; 
            
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
            // 1. この教科に関連する既存データを取得
            List<DepartmentSubject> existingLinks = departmentSubjectRepository.findAll().stream()
                .filter(ds -> ds.getId().getSubjectId().equals(savedSubjectId))
                .collect(Collectors.toList());

            if (departmentId != null) {
                // 2. 今回登録しようとしているキー（クラスと教科の組み合わせ）を作成
                DepartmentSubjectKey targetKey = new DepartmentSubjectKey(departmentId, savedSubjectId);
                
                // 3. 既存データの中に、同じキーのものがあるか探す
                DepartmentSubject targetLink = existingLinks.stream()
                    .filter(ds -> ds.getId().equals(targetKey))
                    .findFirst()
                    .orElse(null);

                if (targetLink != null) {
                    // 【重要】既存データがあるなら「更新」する（削除・再作成はしない！）
                    targetLink.setGrade(grade);
                    departmentSubjectRepository.save(targetLink);
                    
                    // 更新したので、削除リスト（existingLinks）から外す
                    existingLinks.remove(targetLink);
                } else {
                    // 既存データがないなら「新規作成」する
                    DepartmentSubject newLink = new DepartmentSubject();
                    newLink.setId(targetKey);
                    newLink.setGrade(grade);
                    
                    departmentRepository.findById(departmentId).ifPresent(newLink::setDepartment);
                    newLink.setSubject(savedEntity);

                    departmentSubjectRepository.save(newLink);
                }
            }

            // 4. 今回の処理で選ばれなかった（＝クラスが変わって不要になった）古いデータだけを削除
            departmentSubjectRepository.deleteAll(existingLinks);

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

    public List<Map<String, Object>> getSimpleCourseList() {
        return courseRepository.findAll().stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("courseId", c.getCourseId());
            map.put("courseName", c.getCourseName());
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

}