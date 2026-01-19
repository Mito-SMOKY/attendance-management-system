package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.UsersEntity;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, Integer> {

    // emailをキーにしてusersテーブルから1件検索するメソッド
    Optional<UsersEntity> findByEmail(String email);

    // LoginIDをキーにしてusersテーブルから1件検索するメソッド
    Optional<UsersEntity> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    // RoleIDをキーにしてusersテーブルから複数件検索するメソッド
    List<UsersEntity> findByUserTypeId(Integer userTypeId);

    //ユーザーIDから氏名を取得するメソッド
    @Query("SELECT u.name FROM UsersEntity u WHERE u.userId = :userId")
    String findNameByUserId(@Param("userId") Integer userId);
}