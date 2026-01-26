package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.loginandprofile.service.CustomUserDetails;

@Service
public class StudentDepartmentRequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private UsersRepository usersRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    // 申請種別: 学科・コース変更 (4)
    private static final int TYPE_CHANGE_DEPARTMENT = 4;

    // 生徒表示用データの取得
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
            String name = (row[2] != null) ? (String) row[2] : "（不明）";
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

    // 学科・コースリストの取得
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
            
            // ★修正: ハードコードしていた "クラス" を削除しました
            // DBに「Aクラス」と入っていれば「(Aクラス)」と表示され、「A」なら「(A)」となります。
            if (!className.isEmpty()) displayName += " (" + className + ")";
            
            map.put("id", id);
            map.put("name", displayName);
            list.add(map);
        }
        return list;
    }

    // 承認者リストの取得
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

    // 学科・コース変更申請の作成
    @Transactional
    public void createDepartmentRequests(List<Integer> targetIds, Integer targetDepartmentId, String remarks, Integer approverId, CustomUserDetails applicant) {
        List<UsersEntity> targetUsers = usersRepository.findAllById(targetIds);
        if (targetUsers.isEmpty()) return;

        RequestEntity request = new RequestEntity();
        request.setRequestTypeId(TYPE_CHANGE_DEPARTMENT);
        request.setRequesterUserId(applicant.getUserId());
        request.setApproverId(approverId);
        
        request.setTargetDepartmentId(targetDepartmentId); 
        
        request.setRequestMessage(remarks);
        request.setStatus(1); 
        request.setTargetUsers(targetUsers);
        
        requestRepository.save(request);
    }
}