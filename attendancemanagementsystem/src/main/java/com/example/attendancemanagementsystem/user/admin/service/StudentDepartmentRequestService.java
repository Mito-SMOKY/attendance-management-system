package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.dto.DepartmentRequestDto;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Service
public class StudentDepartmentRequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private EnrollmentsRepository enrollmentsRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PersistenceContext
    private EntityManager entityManager;

    // 申請種別: 学科・コース変更 (ID: 4)
    private static final int TYPE_CHANGE_DEPARTMENT = 4;

    /**
     * 確認画面用の生徒データ取得
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudentDisplayData(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();

        String sql = """
            SELECT 
                s.UserID,
                u.LoginID,
                u.Name,
                ss.StudentStatusName,
                e.Grade,
                d.Class,
                m.MajorName,
                c.CourseName,
                CASE 
                    WHEN EXISTS (
                        SELECT 1 
                        FROM request r
                        JOIN requesttargetuser rt ON r.RequestID = rt.RequestID
                        WHERE rt.UserID = s.UserID 
                        AND r.RequestTypeID = 4
                        AND r.Status = 1
                    ) THEN 1 
                    ELSE 0 
                END AS IsPending,
                e.DepartmentID
            FROM student s
            LEFT JOIN users u ON s.UserID = u.UserID
            LEFT JOIN studentstatus ss ON s.StudentStatusID = ss.StudentStatusID 
            LEFT JOIN enrollments e ON s.UserID = e.UserID AND e.IsActive = true
            LEFT JOIN department d ON e.DepartmentID = d.DepartmentID
            LEFT JOIN major m ON d.MajorID = m.MajorID
            LEFT JOIN course c ON m.CourseID = c.CourseID
            WHERE s.UserID IN (:ids)
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("ids", ids);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> displayList = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            
            Integer userId = (Integer) row[0];
            String loginId = (row[1] != null) ? (String) row[1] : "-";
            String name = (row[2] != null) ? (String) row[2] : "（不明）";
            String statusText = (row[3] != null) ? (String) row[3] : "不明";
            String grade = (row[4] != null) ? row[4].toString() : "-";
            String className = (row[5] != null) ? (String) row[5] : "-";
            
            String majorName = (row[6] != null) ? (String) row[6] : "";
            String courseName = (row[7] != null) ? (String) row[7] : "";
            String deptAndCourse = majorName + (courseName.isEmpty() ? "" : "・" + courseName);
            if (deptAndCourse.isEmpty()) deptAndCourse = "-";
            
            boolean isPending = ((Number) row[8]).intValue() == 1;
            Integer currentDeptId = (row[9] != null) ? (Integer) row[9] : 0;

            map.put("userId", userId);
            map.put("loginId", loginId);
            map.put("name", name);
            map.put("statusText", statusText);
            map.put("grade", grade);
            map.put("classroom", className);
            map.put("department", deptAndCourse);
            map.put("isPending", isPending);
            map.put("currentDeptId", currentDeptId);

            displayList.add(map);
        }
        return displayList;
    }

    /**
     * 学科・コースリストの取得
     */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDepartmentList() {
        String sql = """
            SELECT 
                d.DepartmentID, 
                m.MajorName, 
                c.CourseName, 
                d.Class 
            FROM department d
            LEFT JOIN major m ON d.MajorID = m.MajorID
            LEFT JOIN course c ON m.CourseID = c.CourseID
            ORDER BY m.MajorID, c.CourseID, d.Class
        """;
        
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            Integer id = (Integer) row[0];
            String major = (String) row[1];
            String course = (row[2] != null) ? (String) row[2] : "";
            String className = (row[3] != null) ? (String) row[3] : "";
            
            String displayName = major;
            if (!course.isEmpty()) displayName += "・" + course;
            if (!className.isEmpty()) displayName += " (" + className + ")";
            
            map.put("id", id);
            map.put("name", displayName);
            list.add(map);
        }
        return list;
    }

    /**
     * 承認者リストの取得
     */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApproverList() {
        String sql = """
            SELECT u.UserID, u.Name 
            FROM administrator a
            JOIN users u ON a.UserID = u.UserID
            WHERE a.AdminLevelID = 1
        """;
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", row[0]);
            map.put("name", row[1]);
            list.add(map);
        }
        return list;
    }

    /**
     * 学科・コース変更申請の作成 (一般管理者用)
     */
    @Transactional
    public void createDepartmentRequests(DepartmentRequestDto dto, CustomUserDetails applicant) {
        List<UsersEntity> targetUsers = usersRepository.findAllById(dto.getTargetIds());
        if (targetUsers.isEmpty()) return;

        RequestEntity request = new RequestEntity();
        request.setRequestTypeId(TYPE_CHANGE_DEPARTMENT);
        request.setRequesterUserId(applicant.getUserId());
        request.setApproverId(dto.getApproverId());
        request.setTargetDepartmentId(dto.getTargetDepartmentId()); 
        request.setRequestMessage(dto.getRemarks());
        request.setStatus(1); 
        request.setTargetUsers(targetUsers);
        
        requestRepository.save(request);
    }

    /**
     * 学科・コース変更の即時実行 (上位管理者用)
     */
    @Transactional
    public void executeDepartmentUpdate(Map<String, Object> requestData, CustomUserDetails user) {
        // 1. 管理者ユーザーの確認とパスワード検証
        UsersEntity adminUser = usersRepository.findById(user.getUserId())
            .orElseThrow(() -> new SecurityException("管理者ユーザーが見つかりません。"));

        String rawPassword = (String) requestData.get("password");
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, adminUser.getPassword())) {
            throw new SecurityException("パスワードが間違っています。");
        }

        // 2. データ抽出と型変換
        List<?> rawIds = (List<?>) requestData.get("targetIds");
        if (rawIds == null || rawIds.isEmpty()) {
            throw new IllegalArgumentException("対象者が選択されていません。");
        }
        
        List<Integer> targetIds = rawIds.stream()
            .map(obj -> Integer.parseInt(obj.toString()))
            .collect(Collectors.toList());
        
        Object deptIdObj = requestData.get("targetDepartmentId");
        
        // ★修正点: 一時変数で処理してから、final変数に確定させる
        Integer tempId = null;
        if (deptIdObj instanceof String) {
            tempId = Integer.parseInt((String) deptIdObj);
        } else if (deptIdObj instanceof Integer) {
            tempId = (Integer) deptIdObj;
        }
        
        // エラーチェック
        if (tempId == null) {
            throw new IllegalArgumentException("移動先の学科・コースが正しくありません。");
        }
        
        // ラムダ式(orElseThrow)で使うための final変数を用意
        final Integer finalDeptId = tempId;

        // 3. 移動先学科エンティティの取得
        DepartmentEntity newDepartment = departmentRepository.findById(finalDeptId)
            .orElseThrow(() -> new IllegalArgumentException("指定された学科が見つかりません: ID=" + finalDeptId));

        // 4. 更新実行
        for (Integer userId : targetIds) {
            EnrollmentsEntity enrollment = enrollmentsRepository.findByUserIdAndIsActiveTrue(userId);
            if (enrollment != null) {
                // エンティティごとセット
                enrollment.setDepartment(newDepartment);
                enrollmentsRepository.save(enrollment);
            }
        }
    }
}