package com.example.attendancemanagementsystem.user.admin.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.DepartmentEntity; // ★追加
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository; // ★追加
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class RequestListService {

    @Autowired
    private RequestRepository requestRepository;
    
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private EnrollmentsRepository enrollmentsRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository; // ★追加: 学科エンティティ取得用

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 自分宛ての申請一覧を取得
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApprovalList(Integer approverId) {
        List<RequestEntity> requests = requestRepository.findByApproverIdOrderByCreatedAtDesc(approverId);
        List<Map<String, Object>> list = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        for (RequestEntity req : requests) {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", req.getRequestId());
            map.put("createdAt", (req.getCreatedAt() != null) ? req.getCreatedAt().format(dtf) : "-");
            
            String requesterName = "不明";
            if (req.getRequesterUserId() != null) {
                Optional<UsersEntity> requester = usersRepository.findById(req.getRequesterUserId());
                if (requester.isPresent()) {
                    requesterName = requester.get().getName();
                }
            }
            map.put("requesterName", requesterName);
            map.put("type", getRequestTypeName(req.getRequestTypeId()));
            map.put("status", getStatusName(req.getStatus()));
            map.put("statusCode", req.getStatus());
            
            list.add(map);
        }
        return list;
    }

    /**
     * 申請詳細データの取得
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRequestDetail(Integer requestId) {
        RequestEntity req = requestRepository.findById(requestId).orElse(null);
        if (req == null) return null;

        Map<String, Object> map = new HashMap<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        map.put("requestId", req.getRequestId());
        map.put("createdAt", (req.getCreatedAt() != null) ? req.getCreatedAt().format(dtf) : "-");
        
        Integer typeId = req.getRequestTypeId();
        map.put("type", getRequestTypeName(typeId));
        map.put("typeId", typeId);
        map.put("status", getStatusName(req.getStatus()));
        map.put("statusCode", req.getStatus());
        map.put("message", (req.getRequestMessage() != null) ? req.getRequestMessage() : "");

        String requesterName = "不明";
        if (req.getRequesterUserId() != null) {
            usersRepository.findById(req.getRequesterUserId())
                .ifPresent(u -> map.put("requesterName", u.getName()));
        }
        if (!map.containsKey("requesterName")) map.put("requesterName", "不明");

        List<Map<String, String>> students = new ArrayList<>();
        List<UsersEntity> targets = req.getTargetUsers();
        if (targets != null) {
            for (UsersEntity u : targets) {
                Map<String, String> s = new HashMap<>();
                s.put("name", u.getName());
                s.put("loginId", u.getLoginId());
                students.add(s);
            }
        }
        map.put("students", students);
        map.put("targetDetail", getTargetDetailText(req));

        return map;
    }

    /**
     * 承認・却下の実行処理
     */
    @Transactional
    public void processRequest(Integer requestId, boolean isApproved) {
        RequestEntity req = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid Request ID"));

        if (req.getStatus() != 1) {
            throw new IllegalStateException("この申請は既に処理されています。");
        }

        if (isApproved) {
            req.setStatus(2); // 承認済み
            executeDataUpdate(req);
        } else {
            req.setStatus(3); // 却下
        }
        
        requestRepository.save(req);
    }

    private void executeDataUpdate(RequestEntity req) {
        List<UsersEntity> targets = req.getTargetUsers();
        if (targets == null || targets.isEmpty()) return;

        switch (req.getRequestTypeId()) {
            case 2: // アカウント削除
                for (UsersEntity u : targets) {
                    u.setDeleteFlag(true); 
                    usersRepository.save(u);
                }
                break;
            case 3: // ステータス変更
                Integer newStatusId = req.getTargetStatusId();
                if (newStatusId != null) {
                    for (UsersEntity u : targets) {
                        StudentEntity student = studentRepository.findById(u.getUserId()).orElse(null);
                        if (student != null) {
                            student.setStudentStatusId(newStatusId);
                            studentRepository.save(student);
                        }
                    }
                }
                break;
            case 4: // 学科・コース変更
                Integer newDeptId = req.getTargetDepartmentId();
                if (newDeptId != null) {
                    // ★修正: 正しいDepartmentEntityを取得してセットする
                    DepartmentEntity newDept = departmentRepository.findById(newDeptId).orElse(null);
                    
                    if (newDept != null) {
                        for (UsersEntity u : targets) {
                            EnrollmentsEntity enrollment = enrollmentsRepository.findByUserIdAndIsActiveTrue(u.getUserId());
                            if (enrollment != null) {
                                enrollment.setDepartment(newDept); // エンティティを直接セット
                                enrollmentsRepository.save(enrollment);
                            }
                        }
                    }
                }
                break;
        }
    }

    private String getTargetDetailText(RequestEntity req) {
        Integer typeId = req.getRequestTypeId();
        if (typeId == 3 && req.getTargetStatusId() != null) {
            try {
                String sql = "SELECT StudentStatusName FROM studentstatus WHERE StudentStatusID = :id";
                Query query = entityManager.createNativeQuery(sql);
                query.setParameter("id", req.getTargetStatusId());
                return "変更後: " + query.getSingleResult();
            } catch (Exception e) { return "変更後: 不明"; }
        } else if (typeId == 4 && req.getTargetDepartmentId() != null) {
            try {
                String sql = """
                    SELECT CONCAT(m.MajorName, IFNULL(CONCAT('・', c.CourseName), ''), ' (', d.Class, ')')
                    FROM department d
                    LEFT JOIN major m ON d.MajorID = m.MajorID
                    LEFT JOIN course c ON m.CourseID = c.CourseID
                    WHERE d.DepartmentID = :id
                """;
                Query query = entityManager.createNativeQuery(sql);
                query.setParameter("id", req.getTargetDepartmentId());
                return "変更後: " + query.getSingleResult();
            } catch (Exception e) { return "変更後: 不明"; }
        }
        return null;
    }
    
    private String getRequestTypeName(Integer typeId) {
        if (typeId == null) return "-";
        switch (typeId) {
            case 1: return "公欠・欠席";
            case 2: return "アカウント削除";
            case 3: return "ステータス変更";
            case 4: return "学科・コース変更";
            default: return "その他";
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) return "-";
        switch (status) {
            case 1: return "承認待ち";
            case 2: return "承認済み";
            case 3: return "却下";
            default: return "-";
        }
    }
}