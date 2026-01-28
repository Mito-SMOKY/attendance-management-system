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

@Service
public class RequestListService {

    @Autowired
    private RequestRepository requestRepository;
    
    @Autowired
    private UsersRepository usersRepository;

    /**
     * 自分宛ての申請一覧を取得
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getApprovalList(Integer approverId) {
        // 自分宛ての申請を新しい順に取得
        List<RequestEntity> requests = requestRepository.findByApproverIdOrderByCreatedAtDesc(approverId);
        
        List<Map<String, Object>> list = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        for (RequestEntity req : requests) {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", req.getRequestId());
            map.put("createdAt", (req.getCreatedAt() != null) ? req.getCreatedAt().format(dtf) : "-");
            
            // 申請者の名前を取得
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
            map.put("statusCode", req.getStatus()); // 1:承認待ち, 2:承認, 3:拒否
            
            list.add(map);
        }
        return list;
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
            case 2: return "承認済み";
            case 3: return "却下";
            default: return "-";
        }
    }
}