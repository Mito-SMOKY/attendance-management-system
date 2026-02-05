package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.UsersEntity;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, Integer> {

    // メールアドレスでのユーザー取得メソッド
    @Query("SELECT u FROM UsersEntity u WHERE u.email = :email AND u.deleteFlag = false")
    Optional<UsersEntity> findByEmail(@Param("email") String email);

    // ログインIDでのユーザー取得メソッド
    @Query("SELECT u FROM UsersEntity u WHERE u.loginId = :loginId AND u.deleteFlag = false")
    Optional<UsersEntity> findByLoginId(@Param("loginId") String loginId);

    // ログインIDの存在確認メソッド
    boolean existsByLoginId(String loginId);

    // ユーザー種別IDでのユーザー一覧取得メソッド
    @Query("SELECT u FROM UsersEntity u WHERE u.userTypeId = :userTypeId AND u.deleteFlag = false")
    List<UsersEntity> findByUserTypeId(@Param("userTypeId") Integer userTypeId);

    // ユーザー名取得用メソッド
    @Query("SELECT u.name FROM UsersEntity u WHERE u.userId = :userId")
    String findNameByUserId(@Param("userId") Integer userId);

    // メールアドレスがNULLの場合にのみ更新するメソッド
    @Modifying
    @Transactional
    @Query("UPDATE UsersEntity u SET u.email = :email WHERE u.userId = :userId AND u.email IS NULL")
    int updateEmailIfNull(@Param("userId") Integer userId, @Param("email") String email);
}