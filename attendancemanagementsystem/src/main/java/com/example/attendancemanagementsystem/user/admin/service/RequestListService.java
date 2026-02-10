package com.example.attendancemanagementsystem.user.admin.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.AdministratorEntity;
import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AdministratorRepository;
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
    private AdministratorRepository administratorRepository;
    
    @Autowired
    private RequestService requestService;
    
    @PersistenceContext
    private EntityManager entityManager;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private final DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 申請一覧を取得する
     */
    public List<Map<String, Object>> getRequestList() {
        
        List<AdministratorEntity> superAdmins = administratorRepository.findByAdminLevelId(1);
        List<RequestEntity> requests;

        if (superAdmins.isEmpty()) {
            requests = requestRepository.findAll(); 
        } else {
            List<Integer> superAdminIds = superAdmins.stream()
                    .map(AdministratorEntity::getUserId)
                    .collect(Collectors.toList());
            requests = requestRepository.findByRequesterUserIdNotInOrderByCreatedAtDesc(superAdminIds);
        }

        List<Map<String, Object>> resultList = new ArrayList<>();

        for (RequestEntity req : requests) {
            Map<String, Object> m = new HashMap<>();
            
            m.put("requestId", req.getRequestId());
            m.put("requestTypeId", req.getRequestTypeId());
            
            String typeName = getRequestTypeName(req.getRequestTypeId());
            m.put("type", typeName);
            m.put("typeName", typeName);
            
            m.put("statusCode", req.getStatus()); 
            m.put("status", getStatusText(req.getStatus())); 

            m.put("createdAt", req.getCreatedAt() != null ? req.getCreatedAt().format(dtf) : "");
            
            String requesterName = "不明";
            if (req.getRequesterUserId() != null) {
                 UsersEntity user = usersRepository.findById(req.getRequesterUserId()).orElse(null);
                 if (user != null) {
                     requesterName = user.getName();
                 }
            }
            m.put("requesterName", requesterName);
            m.put("requestMessage", req.getRequestMessage());
            
            if (req.getStartDate() != null) m.put("startDate", req.getStartDate().format(dateOnly));
            if (req.getEndDate() != null) m.put("endDate", req.getEndDate().format(dateOnly));
            if (req.getPeriods() != null) m.put("periods", req.getPeriods());
            
            resultList.add(m);
        }

        return resultList;
    }

    /**
     * 申請詳細を取得する
     */
    public Map<String, Object> getRequestDetail(Integer requestId) {
        RequestEntity req = requestRepository.findById(requestId).orElse(null);
        if (req == null) return null;
        
        Map<String, Object> m = new HashMap<>();
        
        // --- 必須項目の設定 ---
        m.put("requestId", req.getRequestId());
        m.put("requesterId", req.getRequesterUserId()); 
        m.put("requestTypeId", req.getRequestTypeId());
        
        String typeName = getRequestTypeName(req.getRequestTypeId());
        m.put("type", typeName);
        m.put("typeName", typeName);
        m.put("typeId", req.getRequestTypeId());

        m.put("statusCode", req.getStatus());
        m.put("status", getStatusText(req.getStatus()));
        
        m.put("createdAt", req.getCreatedAt() != null ? req.getCreatedAt().format(dtf) : "");
        
        String requesterName = "不明";
        if (req.getRequesterUserId() != null) {
             UsersEntity user = usersRepository.findById(req.getRequesterUserId()).orElse(null);
             if (user != null) {
                 requesterName = user.getName();
             }
        }
        m.put("requesterName", requesterName);
        
        m.put("requestMessage", req.getRequestMessage());
        m.put("message", req.getRequestMessage());
        
        // --- オプション項目（nullでもキーを必ず登録する） ---
        // これらがMapに含まれていないとHTML側でアクセスエラーになるため、明示的にputする
        
        m.put("startDate", req.getStartDate() != null ? req.getStartDate().format(dateOnly) : null);
        m.put("endDate", req.getEndDate() != null ? req.getEndDate().format(dateOnly) : null);
        m.put("periods", req.getPeriods());
        
        // 【修正点】条件分岐せず、必ずキーを登録する
        m.put("targetStatusId", req.getTargetStatusId());
        m.put("targetDepartmentId", req.getTargetDepartmentId());
        
        // 詳細情報（日付・時限）の変換
        List<Map<String, Object>> detailsList = null;
        if (req.getRequestDetails() != null && !req.getRequestDetails().isEmpty()) {
            detailsList = req.getRequestDetails().stream().map(d -> {
                Map<String, Object> dm = new HashMap<>();
                dm.put("date", d.getTargetDate() != null ? d.getTargetDate().format(dateOnly) : "");
                dm.put("slot", d.getSlotId());
                return dm;
            }).collect(Collectors.toList());
        }
        // 【修正点】nullまたはリストを必ず登録する
        m.put("details", detailsList);

        return m;
    }

    /**
     * 承認・却下処理
     */
    public void processRequest(Integer requestId, boolean isApproved, Integer approverId) {
        if (isApproved) {
            requestService.approveRequest(requestId, approverId);
        } else {
            RequestEntity req = requestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalArgumentException("申請が見つかりません"));
            
            req.setStatus(3); // 3: 却下
            req.setApproverId(approverId);
            requestRepository.save(req);
        }
    }

    private String getRequestTypeName(Integer typeId) {
        if (typeId == null) return "-";
        switch (typeId) {
            case 1: return "公欠申請";
            case 2: return "アカウント削除申請";
            case 3: return "ステータス変更申請";
            case 4: return "学科コース変更申請";
            default: return "その他(" + typeId + ")";
        }
    }

    private String getStatusText(Integer status) {
        if (status == null) return "-";
        switch (status) {
            case 1: return "承認待ち";
            case 2: return "承認済み";
            case 3: return "却下";
            default: return String.valueOf(status);
        }
    }
}