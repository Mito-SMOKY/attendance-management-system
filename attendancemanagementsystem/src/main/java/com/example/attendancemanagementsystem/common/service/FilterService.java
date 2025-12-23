package com.example.attendancemanagementsystem.common.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class FilterService {

    // 通知タイプフィルタ用
    public <T, V> Specification<T> createMappedInSpec(String inputString, String targetColumn, Map<String, List<V>> mappingGroup) {
        return (root, query, cb) -> {

            // 入力文字列が空の場合は全件対象
            if (inputString == null || inputString.trim().isEmpty()) {
                return cb.conjunction();
            }

            // マッピングから対象IDリストを構築
            List<V> targetIds = new ArrayList<>();
            for (String key : inputString.split(",")) {
                String trimmedKey = key.trim();

                // マップに定義があればそのIDリストを追加
                if (mappingGroup.containsKey(trimmedKey)) {
                    targetIds.addAll(mappingGroup.get(trimmedKey));
                }
            }

            // 対象IDが空の場合はヒットなし
            if (targetIds.isEmpty()) {
                return cb.disjunction();
            }

            // IN検索条件を返す
            return root.get(targetColumn).in(targetIds);
        };
    }

    // ブックマーク・既読ステータスフィルタ用
    public <T> Specification<T> createBooleanStatusSpec(String inputStatus, String column, String trueKey, String falseKey) {
        return (root, query, cb) -> {

            // 入力文字列が空の場合は全件対象
            if (inputStatus == null || inputStatus.trim().isEmpty()) {
                return cb.conjunction();
            }
            
            if (trueKey.equalsIgnoreCase(inputStatus)) {
                return cb.equal(root.get(column), true);
            } else if (falseKey.equalsIgnoreCase(inputStatus)) {
                return cb.equal(root.get(column), false);
            }
            return cb.conjunction();
        };
    }

    // 等価フィルタ用
    public <T> Specification<T> createEqualSpec(String column, Object value) {
        return (root, query, cb) -> {
            if (value == null) return cb.conjunction();
            return cb.equal(root.get(column), value);
        };
    }

    // // IDリストフィルタ用
    // public <T> Specification<T> createIdListSpec(String inputCsv, String targetColumn) {
    //     return (root, query, cb) -> {
    //         if (inputCsv == null || inputCsv.trim().isEmpty()) {
    //             return cb.conjunction();
    //         }

    //         List<Integer> idList = new ArrayList<>();
    //         try {
    //             for (String s : inputCsv.split(",")) {
    //                 idList.add(Integer.parseInt(s.trim()));
    //             }
    //         } catch (NumberFormatException e) {
    //             // 数値以外が混ざっていたら安全のためヒットなしにする
    //             return cb.disjunction();
    //         }

    //         if (idList.isEmpty()) {
    //             return cb.disjunction();
    //         }

    //         return root.get(targetColumn).in(idList);
    //     };
    // }
}