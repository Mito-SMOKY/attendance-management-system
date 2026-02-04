package com.example.attendancemanagementsystem.user.admin.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.RequestDetailEntity;
import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
@Transactional
public class RequestListService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private RequestService requestService;
    
    @PersistenceContext
    private EntityManager entityManager;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private final DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 未承認の申請一覧を取得
     */
    public List<Map<String, Object>> getApprovalList(Integer adminId) {
        // ステータス1(未承認)を取得
        List<RequestEntity> entities = requestRepository.findByStatus(1);
        
        return entities.stream().map(req -> {
            Map<String, Object> m = new HashMap<>();
            m.put("requestId", req.getRequestId());
            m.put("type", getRequestTypeName(req.getRequestTypeId())); // ★ここで正しい名前になります
            m.put("createdAt", req.getCreatedAt().format(dtf));
            m.put("status", "未承認");
            m.put("statusCode", req.getStatus());
            
            // 申請者名の取得
            String userName = usersRepository.findById(req.getRequesterUserId())
                    .map(UsersEntity::getName)
                    .orElse("不明なユーザー");
            m.put("requesterName", userName);
            
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 申請詳細を取得
     */
    public Map<String, Object> getRequestDetail(Integer requestId) {
        RequestEntity req = requestRepository.findById(requestId).orElse(null);
        if (req == null) return null;

        Map<String, Object> m = new HashMap<>();
        m.put("requestId", req.getRequestId());
        m.put("typeId", req.getRequestTypeId());
        m.put("typeName", getRequestTypeName(req.getRequestTypeId()));
        m.put("status", req.getStatus());
        m.put("message", req.getRequestMessage());
        m.put("createdAt", req.getCreatedAt().format(dtf));

        UsersEntity user = usersRepository.findById(req.getRequesterUserId()).orElse(null);
        if (user != null) {
            m.put("requesterName", user.getName());
            m.put("requesterId", user.getUserId());
        } else {
            m.put("requesterName", "不明");
            m.put("requesterId", "");
        }

        // ▼ タイプ別の詳細情報作成 ▼
        
        // Type 1: 公欠・欠席
        if (req.getRequestTypeId() == 1) {
            List<Map<String, Object>> detailList = new ArrayList<>();
            List<RequestDetailEntity> details = req.getRequestDetails();
            
            if (details != null) {
                for (RequestDetailEntity rd : details) {
                    Map<String, Object> d = new HashMap<>();
                    d.put("date", rd.getTargetDate().format(dateOnly));
                    d.put("slot", rd.getSlotId());
                    detailList.add(d);
                }
            }
            m.put("details", detailList);
        }
        
        // Type 4: 学科・コース変更 (詳細な学科名を取得して表示)
        else if (req.getRequestTypeId() == 4 && req.getTargetDepartmentId() != null) {
            m.put("targetDeptId", req.getTargetDepartmentId());
            try {
                // 学科IDから学科名・コース名・クラス名を結合して取得するSQL
                String sql = """
                    SELECT CONCAT(m.MajorName, IFNULL(CONCAT('・', c.CourseName), ''), ' (', d.Class, ')')
                    FROM department d
                    LEFT JOIN major m ON d.MajorID = m.MajorID
                    LEFT JOIN course c ON m.CourseID = c.CourseID
                    WHERE d.DepartmentID = :id
                """;
                Query query = entityManager.createNativeQuery(sql);
                query.setParameter("id", req.getTargetDepartmentId());
                Object result = query.getSingleResult();
                m.put("targetDeptName", result != null ? result.toString() : "不明な学科");
            } catch (Exception e) {
                m.put("targetDeptName", "学科情報の取得失敗");
            }
        }
        
        // Type 3: ステータス変更
        else if (req.getRequestTypeId() == 3 && req.getTargetStatusId() != null) {
             m.put("targetStatusId", req.getTargetStatusId());
             // 必要ならステータス名を取得する処理を追加
        }

        return m;
    }

    /**
     * 承認・却下処理
     */
    public void processRequest(Integer requestId, boolean isApproved, Integer approverId) {
        if (isApproved) {
            // 承認: 公欠(Type1)ならRequestServiceへ。それ以外はここで更新処理が必要かもしれません。
            // ※現状はすべてのタイプで requestService.approveRequest を呼んでいますが、
            // Type 2,3,4 の承認ロジックが RequestService に無い場合は、ここに個別の処理を書く必要があります。
            // (今回は「一覧に出す」ことが目的なので、承認ロジックは既存のままにしています)
            requestService.approveRequest(requestId, approverId);
        } else {
            // 却下
            RequestEntity req = requestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("申請が見つかりません"));
            req.setStatus(3);
            req.setApproverId(approverId);
            requestRepository.save(req);
        }
    }

    // ★★★ ここを修正しました ★★★
    private String getRequestTypeName(Integer typeId) {
        if (typeId == null) return "-";
        switch (typeId) {
            case 1: return "公欠・欠席";
            case 2: return "アカウント削除";      // 修正: 元の定義に戻しました
            case 3: return "ステータス変更";      // 修正: 元の定義に戻しました
            case 4: return "学科・コース変更";    // 修正: 元の定義に戻しました
            default: return "その他";
        }
    }
}