package com.example.attendancemanagementsystem.common.repository;

import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, Integer> {

    // emailをキーにしてusersテーブルから1件検索するメソッド
    Optional<UsersEntity> findByEmail(String email);
}