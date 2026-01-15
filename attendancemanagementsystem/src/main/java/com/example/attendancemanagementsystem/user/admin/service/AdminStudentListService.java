package com.example.attendancemanagementsystem.user.admin.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.service.SearchService;


@Service
public class AdminStudentListService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SearchService searchService;

    /**
     * 生徒一覧を検索・取得し、画面表示用に整形して返す
     */
    public Map<String, Object> searchStudents(int page, int size, String keyword) {

        // 1. ページング設定 (UserId順)
        Pageable pageable = PageRequest.of(page, size, Sort.by("userId").ascending());

        // 2. 検索条件の作成
        Specification<StudentEntity> spec = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            List<String> targetColumns = Arrays.asList(
                "users.loginId", // 学籍番号
                "users.name"     // 氏名
            );
            // 共通検索スペック作成サービスを利用
            spec = searchService.createKeywordSpec(keyword, targetColumns);
        }

        // 3. データ取得
        Page<StudentEntity> studentPage = studentRepository.findAll(spec, pageable);

        // 4. 画面表示用にデータを整形
        List<Map<String, Object>> students = studentPage.getContent().stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            
            // --- 基本情報 (UsersEntity) ---
            if (s.getUsers() != null) {
                map.put("loginId", s.getUsers().getLoginId());
                map.put("name", s.getUsers().getName());
            } else {
                map.put("loginId", "-");
                map.put("name", "不明");
            }

            // --- 在籍情報 (EnrollmentsEntity) ---
            String grade = "-";
            String deptName = "-";
            String classroomName = "-";

            // 在籍情報リストを取得
            List<EnrollmentsEntity> enrollList = s.getEnrollments();
            if (enrollList != null && !enrollList.isEmpty()) {
                // 有効(IsActive=true)かつ最新のデータを取得
                EnrollmentsEntity activeEnrollment = enrollList.stream()
                    .filter(EnrollmentsEntity::isActive)
                    .max(Comparator.comparing(EnrollmentsEntity::getEnrollmentsId))
                    .orElse(null);

                // 有効な在籍情報が見つかった場合、各情報を取得
                if (activeEnrollment != null) {
                    // 学年
                    if (activeEnrollment.getGrade() != null) {
                        grade = activeEnrollment.getGrade() + "年";
                    }

                    // 学科・コース・クラス
                    if (activeEnrollment.getDepartment() != null) {
                        if (activeEnrollment.getDepartment().getClassName() != null) {
                            classroomName = activeEnrollment.getDepartment().getClassName();
                        }
                        if (activeEnrollment.getDepartment().getCourse() != null) {
                            deptName = activeEnrollment.getDepartment().getCourse().getCourseName();
                        }
                    }
                }
            }

            // マップに追加
            map.put("grade", grade);
            map.put("department", deptName);
            map.put("classroom", classroomName);

            return map;
        }).collect(Collectors.toList());

        // 5. 結果セットの作成
        Map<String, Object> response = new HashMap<>();
        response.put("content", students);
        response.put("totalPages", studentPage.getTotalPages());
        response.put("number", studentPage.getNumber());
        response.put("totalElements", studentPage.getTotalElements());

        return response;
    }
}