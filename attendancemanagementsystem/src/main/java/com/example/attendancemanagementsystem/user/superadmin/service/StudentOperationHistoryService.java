package com.example.attendancemanagementsystem.user.superadmin.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.AdministratorEntity;
import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AdministratorRepository; // 追加
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentOperationHistoryService {

    private final RequestRepository requestRepository;
    private final UsersRepository usersRepository;
    private final AdministratorRepository administratorRepository; // 追加

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    /**
     * 生徒操作履歴（スーパー管理者による操作ログ）を取得する
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudentOperationHistory() {
        
        // 1. スーパー管理者(Level=1)の一覧を取得
        List<AdministratorEntity> superAdmins = administratorRepository.findByAdminLevelId(1);
        
        // スーパー管理者がいない場合は空リストを返す
        if (superAdmins.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. スーパー管理者のIDリストを作成
        List<Integer> superAdminIds = superAdmins.stream()
                .map(AdministratorEntity::getUserId)
                .collect(Collectors.toList());

        // 3. そのIDリストに含まれるRequestを取得 (IN検索)
        List<RequestEntity> requests = requestRepository.findByRequesterUserIdInOrderByCreatedAtDesc(superAdminIds);
        
        // --- 以下、既存の変換処理 ---
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (RequestEntity req : requests) {
            Map<String, Object> m = new HashMap<>();
            
            m.put("requestId", req.getRequestId());
            m.put("requestTypeId", req.getRequestTypeId());
            m.put("requestTypeName", getRequestTypeName(req.getRequestTypeId()));
            m.put("status", req.getStatus()); 
            m.put("createdAt", req.getCreatedAt() != null ? req.getCreatedAt().format(dtf) : "");
            
            String requesterName = "不明";
            UsersEntity user = usersRepository.findById(req.getRequesterUserId()).orElse(null);
            if (user != null) {
                requesterName = user.getName();
            }
            m.put("requesterName", requesterName);
            m.put("requestMessage", req.getRequestMessage()); // メッセージ等も表示
            
            resultList.add(m);
        }

        return resultList;
    }

    private String getRequestTypeName(Integer typeId) {
        if (typeId == null) return "-";
        if (typeId == 1) return "公欠届";
        if (typeId == 2) return "アカウント削除";
        if (typeId == 3) return "ステータス変更";
        if (typeId == 4) return "学科変更";
        return "その他(" + typeId + ")";
    }
}