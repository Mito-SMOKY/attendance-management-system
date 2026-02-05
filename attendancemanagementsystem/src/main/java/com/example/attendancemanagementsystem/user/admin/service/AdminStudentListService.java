package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.transaction.annotation.Transactional; 

import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity; // ★追加
import com.example.attendancemanagementsystem.common.repository.CourseRepository;
import com.example.attendancemanagementsystem.common.repository.MajorRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.service.SearchService;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class AdminStudentListService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SearchService searchService;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private CourseRepository courseRepository;

    
    // 2. 検索条件の作成--------------------------------------------------------
        // Specification<StudentEntity> spec = null;
        // if (keyword != null && !keyword.trim().isEmpty()) {
        //     List<String> targetColumns = Arrays.asList(
        //         "user.loginId", // 学籍番号
        //         "user.name"     // 氏名
        //     );
        //     // 共通検索スペック作成サービスを利用
        //     spec = searchService.createKeywordSpec(keyword, targetColumns);
        //----------------------------------------------------------------------

            
    // フィルター選択肢取得
    public Map<String, Object> getFilterOptions() {
        Map<String, Object> options = new HashMap<>();
        
        // 学科一覧 
        List<Map<String, Object>> majors = majorRepository.findAll().stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("majorId", m.getMajorId());
            map.put("majorName", m.getMajorName());
            return map;
        }).collect(Collectors.toList());
        options.put("majors", majors);
        
        // コース一覧 
        List<Map<String, Object>> courses = courseRepository.findAll().stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("courseId", c.getCourseId());
            map.put("courseName", c.getCourseName());
            return map;
        }).collect(Collectors.toList());
        options.put("courses", courses);
        
        return options;
    }

    // 生徒一覧検索
    public Map<String, Object> searchStudents(int page, int size, String keyword, 
                                            Integer departmentId, Integer grade, Integer courseId) {

        // 検索条件の構築 ---
        Specification<StudentEntity> spec = (root, query, cb) -> {
            return cb.equal(root.get("user").get("deleteFlag"), false);
        };

        // キーワード検索
        if (keyword != null && !keyword.isEmpty()) {
            List<String> targetColumns = Arrays.asList("user.name", "user.loginId");
            
            // キーワード検索スペックの作成
            Specification<StudentEntity> searchSpec = searchService.createKeywordSpec(keyword, targetColumns);
            spec = spec.and(searchSpec);
        }

        // フィルター検索 
        if (departmentId != null || grade != null || courseId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<StudentEntity, EnrollmentsEntity> joinEnrollments = root.join("enrollments", JoinType.INNER);
                List<Predicate> predicates = new ArrayList<>();

                // 有効な在籍情報
                predicates.add(cb.equal(joinEnrollments.get("isActive"), true));

                // フィルター: 学科ID
                if (departmentId != null) {
                    predicates.add(cb.equal(
                        joinEnrollments.get("department").get("major").get("majorId"), 
                        departmentId
                    ));
                }

                // フィルター: 学年
                if (grade != null) {
                    predicates.add(cb.equal(joinEnrollments.get("grade"), grade));
                }

                // フィルター: コースID
                if (courseId != null) {
                    predicates.add(cb.equal(
                        joinEnrollments.get("department").get("major").get("course").get("courseId"), 
                        courseId
                    ));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            });
        }

        // ページング付きデータ取得 
        Pageable pageable = PageRequest.of(page, size, Sort.by("userId").ascending());
        Page<StudentEntity> studentPage = studentRepository.findAll(spec, pageable);

        // データ整形
        List<Map<String, Object>> students = studentPage.getContent().stream().map(student -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put("userId", student.getUserId());
            
            // ★修正: UsersEntityを取得してから名前などを取得
            UsersEntity user = student.getUser();
            map.put("name", (user != null) ? user.getName() : "");
            map.put("loginId", (user != null) ? user.getLoginId() : "");

            // 学科・コース・学年・クラス情報の取得
            String gradeStr = "-";
            String deptAndCourseName = "-";
            String classroomName = "-";

            // 最新の有効な在籍情報を取得
            List<EnrollmentsEntity> enrollments = student.getEnrollments();
            if (enrollments != null) {
                EnrollmentsEntity activeEnrollment = enrollments.stream()
                    .filter(e -> Boolean.TRUE.equals(e.getIsActive()))
                    .max((e1, e2) -> e1.getEnrollmentsId().compareTo(e2.getEnrollmentsId()))
                    .orElse(null);

                // 在籍情報が存在する場合に各情報を設定
                if (activeEnrollment != null) {
                    if (activeEnrollment.getGrade() != null) gradeStr = activeEnrollment.getGrade() + "年";
                    if (activeEnrollment.getDepartment() != null) {
                        if (activeEnrollment.getDepartment().getClassName() != null) 
                            classroomName = activeEnrollment.getDepartment().getClassName();
                        
                        String dName = "";
                        String cName = "";

                        // 学科・コース名の取得
                        if (activeEnrollment.getDepartment().getMajor() != null) {
                            dName = activeEnrollment.getDepartment().getMajor().getMajorName();
                            if (activeEnrollment.getDepartment().getMajor().getCourse() != null) {
                                cName = activeEnrollment.getDepartment().getMajor().getCourse().getCourseName();
                            }
                        }
                        // 学科・コース名の結合
                        if (!dName.isEmpty()) {
                            deptAndCourseName = !cName.isEmpty() ? dName +("・") +cName : dName;
                        }
                    }
                }
            }

            // マップに設定
            map.put("grade", gradeStr);
            map.put("department", deptAndCourseName);
            map.put("classroom", classroomName);
            return map;
        }).collect(Collectors.toList());

        // レスポンス生成
        Map<String, Object> response = new HashMap<>();
        response.put("content", students);
        response.put("totalPages", studentPage.getTotalPages());
        response.put("number", studentPage.getNumber());
        response.put("totalElements", studentPage.getTotalElements());

        return response;
    }
}