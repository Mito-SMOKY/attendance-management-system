package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.UsersEntity;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, Integer> {

    // emailをキーにしてusersテーブルから1件検索するメソッド
    Optional<UsersEntity> findByEmail(String email);

    // LoginIDをキーにしてusersテーブルから1件検索するメソッド
    Optional<UsersEntity> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    // ★追加: ユーザータイプIDでリスト検索（教師/管理者一覧の取得用）
    List<UsersEntity> findByUserTypeId(Integer userTypeId);
    
}