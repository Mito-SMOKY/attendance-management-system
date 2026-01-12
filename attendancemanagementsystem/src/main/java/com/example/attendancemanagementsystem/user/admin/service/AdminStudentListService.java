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
import org.springframework.transaction.annotation.Transactional; // ★追加

import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.repository.CourseRepository;
import com.example.attendancemanagementsystem.common.repository.MajorRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.service.SearchService;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true) // ★追加: 検索時のデータ読み込みを安定させる
public class AdminStudentListService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SearchService searchService;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * ★修正版: 検索フィルター用の選択肢（学科一覧・コース一覧）を取得する
     * エンティティをそのまま返すとJSON変換でエラーになるため、Mapに詰め替えて返す
     */
    public Map<String, Object> getFilterOptions() {
        Map<String, Object> options = new HashMap<>();
        
        // 学科一覧 (MajorEntity -> Map)
        List<Map<String, Object>> majors = majorRepository.findAll().stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("majorId", m.getMajorId());
            map.put("majorName", m.getMajorName());
            return map;
        }).collect(Collectors.toList());
        options.put("majors", majors);
        
        // コース一覧 (CourseEntity -> Map)
        List<Map<String, Object>> courses = courseRepository.findAll().stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("courseId", c.getCourseId());
            map.put("courseName", c.getCourseName());
            return map;
        }).collect(Collectors.toList());
        options.put("courses", courses);
        
        return options;
    }

    /**
     * 生徒一覧を検索・取得し、画面表示用に整形して返す
     */
    public Map<String, Object> searchStudents(int page, int size, String keyword, 
                                            Integer departmentId, Integer grade, Integer courseId) {

        // --- 1. 検索条件 (Specification) の構築 ---
        Specification<StudentEntity> spec = Specification.where(null);

        // A. キーワード検索
        if (keyword != null && !keyword.isEmpty()) {
            List<String> targetColumns = Arrays.asList("users.name", "users.loginId");
            // SearchServiceを利用
            Specification<StudentEntity> searchSpec = searchService.createKeywordSpec(keyword, targetColumns);
            spec = spec.and(searchSpec);
        }

        // B. フィルター検索 (EnrollmentsテーブルをJOIN)
        if (departmentId != null || grade != null || courseId != null) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<StudentEntity, EnrollmentsEntity> joinEnrollments = root.join("enrollments", JoinType.INNER);
                List<Predicate> predicates = new ArrayList<>();

                // 条件: 有効な在籍情報
                predicates.add(cb.equal(joinEnrollments.get("isActive"), true));

                // フィルター: 学科ID (MajorIDとして検索)
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

        // --- 2. ページングと検索実行 ---
        Pageable pageable = PageRequest.of(page, size, Sort.by("userId").ascending());
        Page<StudentEntity> studentPage = studentRepository.findAll(spec, pageable);

        // --- 3. データ整形 ---
        List<Map<String, Object>> students = studentPage.getContent().stream().map(student -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put("userId", student.getUserId());
            map.put("name", (student.getUsers() != null) ? student.getUsers().getName() : "");
            map.put("loginId", (student.getUsers() != null) ? student.getUsers().getLoginId() : "");

            // 学科・コース・学年・クラス情報の取得
            String gradeStr = "-";
            String deptAndCourseName = "-";
            String classroomName = "-";

            List<EnrollmentsEntity> enrollments = student.getEnrollments();
            if (enrollments != null) {
                EnrollmentsEntity activeEnrollment = enrollments.stream()
                    .filter(e -> Boolean.TRUE.equals(e.getIsActive()))
                    .max((e1, e2) -> e1.getEnrollmentsId().compareTo(e2.getEnrollmentsId()))
                    .orElse(null);

                if (activeEnrollment != null) {
                    if (activeEnrollment.getGrade() != null) gradeStr = activeEnrollment.getGrade() + "年";
                    if (activeEnrollment.getDepartment() != null) {
                        if (activeEnrollment.getDepartment().getClassName() != null) 
                            classroomName = activeEnrollment.getDepartment().getClassName();
                        
                        String dName = "";
                        String cName = "";
                        if (activeEnrollment.getDepartment().getMajor() != null) {
                            dName = activeEnrollment.getDepartment().getMajor().getMajorName();
                            if (activeEnrollment.getDepartment().getMajor().getCourse() != null) {
                                cName = activeEnrollment.getDepartment().getMajor().getCourse().getCourseName();
                            }
                        }
                        if (!dName.isEmpty()) {
                            deptAndCourseName = !cName.isEmpty() ? dName + " / " + cName : dName;
                        }
                    }
                }
            }
            map.put("grade", gradeStr);
            map.put("department", deptAndCourseName);
            map.put("classroom", classroomName);
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", students);
        response.put("totalPages", studentPage.getTotalPages());
        response.put("number", studentPage.getNumber());
        response.put("totalElements", studentPage.getTotalElements());

        return response;
    }
}