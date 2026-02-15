package com.example.attendancemanagementsystem.user.admin.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    private com.example.attendancemanagementsystem.user.admin.service.RequestService adminRequestService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 【自身の申請履歴】 (Outbox)
     * 自分が申請者(Requester)であるデータを取得
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyRequestHistory(Integer userId) {
        // 自分が申請したものを取得
        List<RequestEntity> requests = requestRepository.findByRequesterUserIdOrderByCreatedAtDesc(userId);
        return convertRequestsToMapList(requests, false);
    }

    /**
     * 【生徒からの未承認申請】 (Inbox)
     * ステータスが1(承認待ち) かつ 申請者が生徒(UserTypeId=2) のデータを取得
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingStudentRequests() {
        // 1. 生徒(UserTypeId=2)のユーザー一覧を取得
        List<UsersEntity> students = usersRepository.findByUserTypeId(1);
        
        // 生徒がいない場合は空リストを返す（これを行わないと次の検索でエラーになる可能性があるため）
        if (students.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 生徒のIDリストを抽出
        List<Integer> studentIds = students.stream()
                .map(UsersEntity::getUserId)
                .collect(Collectors.toList());

        // 3. ステータスが1(承認待ち)かつ、申請者が生徒IDリストに含まれるものを検索
        // 追加したRepositoryメソッドを使用
        List<RequestEntity> requests = requestRepository.findByStatusAndRequesterUserIdInOrderByCreatedAtAsc(1, studentIds);

        return convertRequestsToMapList(requests, true);
    }

    // --- 共通変換ロジック ---
    private List<Map<String, Object>> convertRequestsToMapList(List<RequestEntity> requests, boolean showRequesterName) {
        List<Map<String, Object>> list = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        for (RequestEntity req : requests) {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", req.getRequestId());
            map.put("createdAt", (req.getCreatedAt() != null) ? req.getCreatedAt().format(dtf) : "-");
            map.put("type", getRequestTypeName(req.getRequestTypeId()));
            map.put("status", getStatusName(req.getStatus()));
            map.put("statusCode", req.getStatus());
            
            String targetName = "-";
            if (showRequesterName) {
                // 生徒の名前を表示
                targetName = usersRepository.findById(req.getRequesterUserId())
                        .map(UsersEntity::getName).orElse("不明");
            } else {
                // 承認者の名前を表示
                if (req.getApproverId() != null) {
                    targetName = usersRepository.findById(req.getApproverId())
                            .map(UsersEntity::getName).orElse("-");
                }
            }
            map.put("targetName", targetName);
            
            list.add(map);
        }
        return list;
    }

    // --- 詳細取得 ---
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

        String requesterName = usersRepository.findById(req.getRequesterUserId())
                .map(UsersEntity::getName).orElse("不明");
        map.put("requesterName", requesterName);

        String approverName = "-";
        if (req.getApproverId() != null) {
            approverName = usersRepository.findById(req.getApproverId())
                    .map(UsersEntity::getName).orElse("-");
        }
        map.put("approverName", approverName);

        List<Map<String, String>> students = new ArrayList<>();
        List<UsersEntity> targets = req.getTargetUsers();
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

        map.put("targetDetail", null);
        if (typeId != null) {
            if (typeId == 3 && req.getTargetStatusId() != null) {
                try {
                    String sql = "SELECT StudentStatusName FROM studentstatus WHERE StudentStatusID = :id";
                    Query query = entityManager.createNativeQuery(sql);
                    query.setParameter("id", req.getTargetStatusId());
                    String statusName = (String) query.getSingleResult();
                    map.put("targetDetail", "変更後: " + statusName);
                } catch (Exception e) { map.put("targetDetail", "変更後: 不明"); }
            }
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
                    map.put("targetDetail", "変更後: " + deptName);
                } catch (Exception e) { map.put("targetDetail", "変更後: 不明"); }
            }
        }
        return map;
    }

    // --- アクション処理 ---

    // 1. 自身の申請取り下げ
    @Transactional
    public void withdrawRequest(Integer requestId, Integer userId) {
        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("指定された申請が見つかりません"));
        if (!request.getRequesterUserId().equals(userId)) {
            throw new SecurityException("他人の申請を取り下げることはできません。");
        }
        if ((request.getStatus() != null ? request.getStatus() : 0) != 1) { 
            throw new IllegalStateException("既に処理済みのため、取り下げできません。");
        }
        request.setStatus(3); // 3: 取り下げ
        requestRepository.save(request);
    }

    // 2. 生徒の申請を承認
    @Transactional
    public void approveRequest(Integer requestId, Integer approverId) {
        adminRequestService.approveRequest(requestId, approverId);
    }

    // 3. 生徒の申請を却下
    @Transactional
    public void rejectRequest(Integer requestId, Integer approverId) {
        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("申請が見つかりません"));
        if (request.getStatus() != 1) {
            throw new IllegalStateException("既に処理済みの申請です");
        }
        request.setStatus(3); // 3: 却下
        request.setApproverId(approverId);
        requestRepository.save(request);
    }

    // --- ヘルパー ---
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