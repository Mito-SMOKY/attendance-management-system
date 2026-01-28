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

import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class RequestHistoryService {

    @Autowired
    private RequestRepository requestRepository;
    
    @Autowired
    private UsersRepository usersRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // 自分の申請履歴一覧の取得
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyRequestHistory(Integer userId) {
        List<RequestEntity> requests = requestRepository.findByRequesterUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> historyList = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        for (RequestEntity req : requests) {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", req.getRequestId());
            map.put("createdAt", (req.getCreatedAt() != null) ? req.getCreatedAt().format(dtf) : "-");
            map.put("type", getRequestTypeName(req.getRequestTypeId()));
            map.put("status", getStatusName(req.getStatus()));
            map.put("statusCode", req.getStatus());
            
            String approverName = "不明";
            if (req.getApproverId() != null) {
                Optional<UsersEntity> approver = usersRepository.findById(req.getApproverId());
                if (approver.isPresent()) {
                    approverName = approver.get().getName();
                }
            }
            map.put("approverName", approverName);
            historyList.add(map);
        }
        return historyList;
    }

    // 申請詳細情報の取得
    @Transactional(readOnly = true)
    public Map<String, Object> getRequestDetail(Integer requestId) {
        RequestEntity req = requestRepository.findById(requestId).orElse(null);
        if (req == null) return null;

        Map<String, Object> map = new HashMap<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        map.put("requestId", req.getRequestId());
        map.put("createdAt", (req.getCreatedAt() != null) ? req.getCreatedAt().format(dtf) : "-");
        
        Integer typeId = req.getRequestTypeId(); // null対策のため一度変数へ
        map.put("type", getRequestTypeName(typeId));
        map.put("typeId", typeId);
        
        map.put("status", getStatusName(req.getStatus()));
        map.put("statusCode", req.getStatus());
        map.put("message", (req.getRequestMessage() != null) ? req.getRequestMessage() : "");

        // 承認者名
        String approverName = "-";
        if (req.getApproverId() != null) {
            Optional<UsersEntity> u = usersRepository.findById(req.getApproverId());
            if (u.isPresent()) approverName = u.get().getName();
        }
        map.put("approverName", approverName);

        // 対象生徒リスト (LazyInit対策として、List取得時のnullチェックとループを安全に)
        List<Map<String, String>> students = new ArrayList<>();
        List<UsersEntity> targets = req.getTargetUsers(); // ここで取得
        
        if (targets != null && !targets.isEmpty()) {
            for (UsersEntity u : targets) {
                if (u != null) {
                    Map<String, String> s = new HashMap<>();
                    s.put("name", (u.getName() != null) ? u.getName() : "不明");
                    s.put("loginId", (u.getLoginId() != null) ? u.getLoginId() : "-");
                    students.add(s);
                }
            }
        }
        map.put("students", students);

        // 追加情報の取得 (申請種別ごと)
        map.put("targetDetail", null);

        if (typeId != null) {
            // ステータス変更申請(3)
            if (typeId == 3 && req.getTargetStatusId() != null) {
                try {
                    String sql = "SELECT StudentStatusName FROM studentstatus WHERE StudentStatusID = :id";
                    Query query = entityManager.createNativeQuery(sql);
                    query.setParameter("id", req.getTargetStatusId());
                    String statusName = (String) query.getSingleResult();
                    map.put("targetDetail", "変更後: " + statusName);
                } catch (Exception e) {
                    map.put("targetDetail", "変更後: 不明");
                }
            }
            // 学科変更申請(4)
            else if (typeId == 4 && req.getTargetDepartmentId() != null) {
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
                    String deptName = (String) query.getSingleResult();
                    // 表示名から「クラス」のような重複文字を削除する処理が必要ならここで行う
                    // 今回はSQLで整形済み
                    map.put("targetDetail", "変更後: " + deptName);
                } catch (Exception e) {
                    map.put("targetDetail", "変更後: 不明");
                }
            }
        }

        return map;
    }

    // --- ヘルパーメソッド ---

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
            case 2: return "承 認";
            case 3: return "拒 否";
            default: return "その他";
        }
    }
}