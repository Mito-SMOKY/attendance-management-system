package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.dto.DeleteRequestDto;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Service
public class StudentDeleteRequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PersistenceContext
    private EntityManager entityManager;

    // 申請種別: アカウント削除 (ID: 2)
    private static final int TYPE_DELETE_ACCOUNT = 2;

    /**
     * 確認画面用の生徒データ取得
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudentDisplayData(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        String sql = """
            SELECT 
                s.UserID,
                u.LoginID,
                u.Name,
                ss.StudentStatusName,
                e.Grade,
                d.Class,
                m.MajorName,
                c.CourseName
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
            String name = (row[2] != null) ? (String) row[2] : "（データ不整合）";
            String statusText = (row[3] != null) ? (String) row[3] : "不明";
            String grade = (row[4] != null) ? row[4].toString() : "-";
            String className = (row[5] != null) ? (String) row[5] : "-";
            
            String majorName = (row[6] != null) ? (String) row[6] : "";
            String courseName = (row[7] != null) ? (String) row[7] : "";
            String deptAndCourse = majorName + (courseName.isEmpty() ? "" : "・" + courseName);
            if (deptAndCourse.isEmpty()) deptAndCourse = "-";

            map.put("userId", userId);
            map.put("loginId", loginId);
            map.put("name", name);
            map.put("statusText", statusText);
            map.put("grade", grade);
            map.put("classroom", className);
            map.put("department", deptAndCourse);

            displayList.add(map);
        }
        return displayList;
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
     * 生徒アカウント削除申請の作成 (一般管理者用)
     */
    @Transactional
    public void createDeleteRequests(DeleteRequestDto dto, CustomUserDetails applicant) {
        List<UsersEntity> targetUsers = usersRepository.findAllById(dto.getTargetIds());
        if (targetUsers.isEmpty()) return;

        RequestEntity request = new RequestEntity();
        request.setRequestTypeId(TYPE_DELETE_ACCOUNT);
        request.setRequesterUserId(applicant.getUserId());
        request.setApproverId(dto.getApproverId()); 
        request.setRequestMessage(dto.getRemarks());
        request.setStatus(1); // 申請中
        request.setTargetUsers(targetUsers);
        
        requestRepository.save(request);
    }

    /**
     * 削除の即時実行 (上位管理者用)
     * パスワード認証を行い、論理削除フラグを立てる
     */
    @Transactional
    public void executeDelete(Map<String, Object> requestData, CustomUserDetails user) {
        // ★修正点: DBから最新のユーザー情報を取得してパスワードを確認する
        UsersEntity adminUser = usersRepository.findById(user.getUserId())
            .orElseThrow(() -> new SecurityException("管理者ユーザーが見つかりません。"));

        // 1. パスワード確認
        String rawPassword = (String) requestData.get("password");
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, adminUser.getPassword())) {
            throw new SecurityException("パスワードが間違っています。");
        }

        // 2. データ抽出
        @SuppressWarnings("unchecked")
        List<Integer> targetIds = (List<Integer>) requestData.get("targetIds");
        
        if (targetIds == null || targetIds.isEmpty()) {
            throw new IllegalArgumentException("削除対象が選択されていません。");
        }

        // 3. 削除処理 (論理削除)
        List<UsersEntity> users = usersRepository.findAllById(targetIds);
        for (UsersEntity u : users) {
            u.setDeleteFlag(true); // DeleteFlagをtrueに設定
        }
        usersRepository.saveAll(users);

        RequestEntity history = new RequestEntity();
        history.setRequestTypeId(TYPE_DELETE_ACCOUNT);
        history.setRequesterUserId(user.getUserId());
        history.setStatus(2); 

        String remarks = (String) requestData.get("remarks");
        history.setRequestMessage(remarks != null ? remarks : "上位管理者による即時削除");

        // 対象ユーザーは既存処理の users リストをそのまま利用
        history.setTargetUsers(users);

        requestRepository.save(history);
    }
}