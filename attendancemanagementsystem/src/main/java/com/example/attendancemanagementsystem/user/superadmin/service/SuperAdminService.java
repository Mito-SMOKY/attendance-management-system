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
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AdministratorRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.common.service.SearchService;
import com.example.attendancemanagementsystem.user.superadmin.dto.SuperAdminDetailDto;

import jakarta.persistence.criteria.JoinType;

@Service
public class SuperAdminService {

    @Autowired
    private AdministratorRepository administratorRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private SearchService searchService;
    
    // ※削除機能に関連するRepositoryやPasswordEncoderは一旦除外しました

    // --- 一覧取得メソッド（修正済み） ---
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

        // ---------------------------------------------------------
        // ▼▼▼ 修正点1: 検索ロジックの強化（ID検索対応） ▼▼▼
        // ---------------------------------------------------------
        if (keyword != null && !keyword.trim().isEmpty()) {
            String cleanKeyword = keyword.trim();
            
            // A. 文字列カラム（氏名、ログインID）の検索条件を作成
            List<String> targetColumns = Arrays.asList("user.name", "user.loginId");
            Specification<AdministratorEntity> textSpec = searchService.createKeywordSpec(cleanKeyword, targetColumns);
            
            Specification<AdministratorEntity> finalKeywordSpec = textSpec;

            // B. もしキーワードが「数字」なら、ID検索もOR条件で追加する
            if (cleanKeyword.matches("\\d+")) {
                try {
                    int searchId = Integer.parseInt(cleanKeyword);
                    Specification<AdministratorEntity> idSpec = (root, query, cb) -> 
                        cb.equal(root.get("userId"), searchId);
                    
                    // (名前 or ID or ログインID) の形にする
                    finalKeywordSpec = Specification.where(textSpec).or(idSpec);
                } catch (NumberFormatException e) {
                    // 数値変換できなければ無視
                }
            }
            
            spec = spec.and(finalKeywordSpec);
        }

        // ---------------------------------------------------------
        // ▼▼▼ 修正点2: 権限フィルターの修正（管理者のみを追加） ▼▼▼
        // ---------------------------------------------------------
        if ("1".equals(authFilter)) {
            // 上位管理者のみ
            spec = spec.and((root, query, cb) -> cb.equal(root.get("adminLevelId"), 1));
        } else if ("2".equals(authFilter)) {
            // ★追加: 管理者のみ (ここが抜けていたため修正)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("adminLevelId"), 2));
        }

        // 2. データベース検索実行 (Specification + Sort)
        List<AdministratorEntity> entities = administratorRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "userId"));
        
        // 3. 画面表示用データに変換
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (AdministratorEntity admin : entities) {
            if (admin.getUser() == null) continue;

            Map<String, Object> map = new HashMap<>();
            map.put("UserID", admin.getUserId());
            map.put("Name", admin.getUser().getName());
            
            // 上位管理者かどうか判定
            boolean isSuperAdmin = (admin.getAdminLevelId() == 1);
            map.put("authority", isSuperAdmin ? "1" : "0"); 

            resultList.add(map);
        }

        return resultList;
    }

    // --- 詳細取得メソッド ---
    @Transactional(readOnly = true)
    public SuperAdminDetailDto getAdminDetail(Integer id) {
        AdministratorEntity admin = administratorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("管理者が見つかりません ID: " + id));
        
        SuperAdminDetailDto dto = new SuperAdminDetailDto();
        dto.setUserId(admin.getUserId());
        dto.setName(admin.getUser().getName());
        dto.setEmail(admin.getUser().getEmail());
        // DB値: 1=上位, 2=一般 -> 画面値: 1=上位, 0=一般
        dto.setAdminLevelID(admin.getAdminLevelId() == 1 ? 1 : 0);
        
        return dto;
    }

    // --- 更新メソッド ---
    @Transactional
    public void updateAdmin(SuperAdminDetailDto dto) {
        AdministratorEntity admin = administratorRepository.findById(dto.getUserId())
            .orElseThrow(() -> new RuntimeException("管理者が見つかりません"));
        
        UsersEntity user = admin.getUser();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        
        // 権限の変換 (画面0 -> DB2)
        int dbLevel = (dto.getAdminLevelID() == 1) ? 1 : 2;
        admin.setAdminLevelId(dbLevel);

        usersRepository.save(user);
        administratorRepository.save(admin);
    }
}