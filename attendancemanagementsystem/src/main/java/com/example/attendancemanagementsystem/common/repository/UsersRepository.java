package com.example.attendancemanagementsystem.common.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Integer> {

    // emailをキーにしてusersテーブルから1件検索するメソッド
    Optional<Users> findByEmail(String email);

    // LoginIDをキーにしてusersテーブルから1件検索するメソッド
    Optional<Users> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
    
}