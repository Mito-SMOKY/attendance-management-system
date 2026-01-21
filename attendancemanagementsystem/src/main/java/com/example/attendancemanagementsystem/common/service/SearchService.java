package com.example.attendancemanagementsystem.common.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Service
public class SearchService {

    // --- 類義語辞書（全機能共通） ---
    private static final Map<String, List<String>> SYNONYM_MAP = new HashMap<>();
    static {
       // メアド関連
        SYNONYM_MAP.put("メアド", Arrays.asList("メールアドレス"));
        SYNONYM_MAP.put("メールアドレス", Arrays.asList("メアド"));

        // パスワード関連
        SYNONYM_MAP.put("パスワード", Arrays.asList("パス", "pwd", "暗証番号"));
        SYNONYM_MAP.put("pwd", Arrays.asList("パスワード", "パス"));
        SYNONYM_MAP.put("パス", Arrays.asList("パスワード", "pwd"));
        SYNONYM_MAP.put("暗証番号", Arrays.asList("パスワード"));

        // 休講関連
        SYNONYM_MAP.put("休講", Arrays.asList("授業変更", "キャンセル"));
        SYNONYM_MAP.put("キャンセル", Arrays.asList("休講"));
    }

    // 共通キーワード検索スペック作成メソッド
    public <T> Specification<T> createKeywordSpec(String keyword, List<String> targetColumns) {
        return (root, query, cb) -> {
            
            // キーワードが空の場合は常にTrueを返す
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction(); // 常にTrue (AND条件の初期値)
            }

            // キーワードを空白で分割
            String[] words = keyword.trim().split("[\\s　]+");
            List<Predicate> predicates = new ArrayList<>();

            // 各単語について検索条件を作成 (AND検索)
            for (String word : words) {
                
                // 検索語群を作成（入力語 + 類義語）
                List<String> searchTerms = new ArrayList<>();
                searchTerms.add(word);
                if (SYNONYM_MAP.containsKey(word)) {
                    searchTerms.addAll(SYNONYM_MAP.get(word));
                }

                // 単語ごとのOR条件を作成
                Predicate wordMatch = cb.disjunction();

                // 各検索語について
                for (String term : searchTerms) {
                    String likePattern = "%" + term + "%";
                    
                    // 指定された全カラムに対してLIKE検索
                    for (String column : targetColumns) {
                        wordMatch = cb.or(wordMatch, cb.like(getPath(root, column), likePattern));
                    }
                }
                
                // AND条件に追加
                predicates.add(wordMatch);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    // 補助メソッド: RootからPathを取得（ネスト対応）
    private <T> Path<String> getPath(Root<T> root, String attributeName) {
        Path<?> path = root;
        if (attributeName.contains(".")) {
            for (String part : attributeName.split("\\.")) {
                path = path.get(part);
            }
        } else {
            path = path.get(attributeName);
        }
        @SuppressWarnings("unchecked")
        Path<String> stringPath = (Path<String>) path;
        return stringPath;
    }
}