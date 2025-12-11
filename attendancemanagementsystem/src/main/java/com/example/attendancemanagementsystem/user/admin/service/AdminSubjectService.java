package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList; 
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.DepartmentSubject;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;//
import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.user.admin.dto.SubjectMatrixRowDTO;


//教科マスタ系？
@Service
public class AdminSubjectService {

    @Autowired private SubjectRepository subjectRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private DepartmentSubjectRepository departmentSubjectRepository;

    // --- 1. 全クラス名のリスト取得 ---
    public List<String> getAllClassNames() {
        return departmentRepository.findAll().stream()
                .map(DepartmentEntity::getClassName) // DepartmentEntityから取得
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // --- 2. 教科マトリクスデータの取得 ---
    public List<SubjectMatrixRowDTO> getSubjectMatrixData() {
        List<SubjectEntity> subjects = subjectRepository.findAll();
        List<DepartmentEntity> departments = departmentRepository.findAll();
        List<DepartmentSubject> relations = departmentSubjectRepository.findAll();

        List<SubjectMatrixRowDTO> rows = new ArrayList<>();

        for (SubjectEntity sub : subjects) {
            Map<String, Boolean> statusMap = new HashMap<>();
            Integer grade = null; 

            for (DepartmentEntity dept : departments) {
                // 関連チェック
                boolean isRelated = relations.stream()
                        .anyMatch(r -> r.getSubject().getSubjectId().equals(sub.getSubjectId()) 
                                    && r.getDepartment().getDepartmentId().equals(dept.getDepartmentId()));
                
                // クラス名ごとにマップに入れる (上書きされてもOKな仕様とする)
                statusMap.put(dept.getClassName(), isRelated);

                if (grade == null && isRelated) {
                    grade = relations.stream()
                            .filter(r -> r.getSubject().getSubjectId().equals(sub.getSubjectId()) 
                                      && r.getDepartment().getDepartmentId().equals(dept.getDepartmentId()))
                            .findFirst()
                            .map(DepartmentSubject::getGrade)
                            .orElse(null);
                }
            }
            rows.add(new SubjectMatrixRowDTO(sub.getSubjectId(), sub.getSubjectName(), grade, statusMap));
        }
        return rows;
    }
}