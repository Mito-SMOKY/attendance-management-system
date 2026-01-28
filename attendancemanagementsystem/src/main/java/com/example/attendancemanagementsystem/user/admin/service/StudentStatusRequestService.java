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
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.dto.StatusRequestDto;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Service
public class StudentStatusRequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PersistenceContext
    private EntityManager entityManager;

    // 申請種別: ステータス変更 (ID: 3)
    private static final int TYPE_CHANGE_STATUS = 3;

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
                c.CourseName,
                CASE 
                    WHEN EXISTS (
                        SELECT 1 FROM request r
                        JOIN requesttargetuser rt ON r.RequestID = rt.RequestID
                        WHERE rt.UserID = s.UserID 
                            AND r.RequestTypeID = 3
                            AND r.Status = 1
                    ) THEN 1 
                    ELSE 0 
                END AS IsPending
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
            
            boolean isPending = ((Number) row[8]).intValue() == 1;

            map.put("userId", userId);
            map.put("loginId", loginId);
            map.put("name", name);
            map.put("statusText", statusText);
            map.put("grade", grade);
            map.put("classroom", className);
            map.put("department", deptAndCourse);
            map.put("isPending", isPending);

            displayList.add(map);
        }
        return displayList;
    }

    /**
     * ステータス一覧の取得 (ドロップダウン用)
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getStatusList() {
        String sql = "SELECT StudentStatusID, StudentStatusName FROM studentstatus ORDER BY StudentStatusID";
        Query query = entityManager.createNativeQuery(sql);
        
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", row[0]);
            map.put("name", row[1]);
            list.add(map);
        }
        return list;
    }

    /**
     * 承認者(上位管理者)リストの取得
     */
    @SuppressWarnings("unchecked")
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
     * ステータス変更申請の作成処理 (一般管理者用)
     */
    @Transactional
    public void createStatusRequests(StatusRequestDto dto, CustomUserDetails applicant) {
        List<UsersEntity> targetUsers = usersRepository.findAllById(dto.getTargetIds());
        if (targetUsers.isEmpty()) return;

        RequestEntity request = new RequestEntity();
        request.setRequestTypeId(TYPE_CHANGE_STATUS); 
        request.setRequesterUserId(applicant.getUserId());
        request.setApproverId(dto.getApproverId());
        request.setTargetStatusId(dto.getTargetStatusId());
        request.setRequestMessage(dto.getRemarks());
        request.setStatus(1); // 申請中
        request.setTargetUsers(targetUsers);
        
        requestRepository.save(request);
    }

    /**
     * ステータス変更の即時実行処理 (上位管理者用)
     * パスワード認証を行い、成功したらDBを直接更新する
     */
    @Transactional
    public void executeStatusUpdate(Map<String, Object> requestData, CustomUserDetails user) {
        // ★修正点: セッションからではなく、DBから最新のユーザー情報を取得してパスワードを確認する
        // (Spring Securityが認証後にセッション内のパスワードを消去する場合があるため)
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
        
        Object statusIdObj = requestData.get("targetStatusId");
        Integer targetStatusId = null;
        if (statusIdObj instanceof String) {
            targetStatusId = Integer.parseInt((String) statusIdObj);
        } else if (statusIdObj instanceof Integer) {
            targetStatusId = (Integer) statusIdObj;
        }

        if (targetIds == null || targetIds.isEmpty() || targetStatusId == null) {
            throw new IllegalArgumentException("必要なデータが不足しています。");
        }

        // 3. 更新実行
        List<StudentEntity> students = studentRepository.findAllById(targetIds);
        for (StudentEntity student : students) {
            student.setStudentStatusId(targetStatusId);
        }
        studentRepository.saveAll(students);
    }
}