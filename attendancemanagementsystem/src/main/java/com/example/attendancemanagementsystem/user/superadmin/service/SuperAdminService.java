package com.example.attendancemanagementsystem.user.superadmin.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.AdministratorEntity;
import com.example.attendancemanagementsystem.common.repository.AdministratorRepository;
import com.example.attendancemanagementsystem.common.service.SearchService; // ★追加

import jakarta.persistence.criteria.JoinType;

@Service
public class SuperAdminService {

    @Autowired
    private AdministratorRepository administratorRepository;

    @Autowired
    private SearchService searchService; // ★追加

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAdminList(String keyword, String authFilter) {
        
        // 1. 検索条件(Specification)の構築
        Specification<AdministratorEntity> spec = Specification.where(null);

        // N+1問題対策: userテーブルをJOIN FETCHする条件を追加
        spec = spec.and((root, query, cb) -> {
            if (Long.class != query.getResultType()) { // countクエリでない場合のみfetch
                root.fetch("user", JoinType.LEFT);
            }
            return cb.conjunction();
        });

        // キーワード検索 (SearchService利用)
        // user.name (氏名) と user.loginId (ログインID) を検索対象にする
        if (keyword != null && !keyword.isEmpty()) {
            List<String> targetColumns = Arrays.asList("user.name", "user.loginId");
            Specification<AdministratorEntity> keywordSpec = searchService.createKeywordSpec(keyword, targetColumns);
            spec = spec.and(keywordSpec);
        }

        // 権限フィルター (完全一致なので独自に追加)
        if ("1".equals(authFilter)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("adminLevelId"), 1));
        }

        // 2. データベース検索実行 (Specification + Sort)
        List<AdministratorEntity> entities = administratorRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "userId"));
        
        // 3. 画面表示用データに変換
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (AdministratorEntity admin : entities) {
            // User情報がないデータは除外
            if (admin.getUser() == null) continue;

            Map<String, Object> map = new HashMap<>();
            map.put("UserID", admin.getUserId());
            map.put("Name", admin.getUser().getName());
            // 上位管理者かどうか
            boolean isSuperAdmin = (admin.getAdminLevelId() == 1);
            map.put("authority", isSuperAdmin ? "1" : "0"); 

            resultList.add(map);
        }

        return resultList;
    }
}