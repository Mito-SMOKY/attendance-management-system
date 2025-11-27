package com.example.attendancemanagementsystem.common.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Entityパスのインポート
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Integer> {

    // UsersEntity をもとに StudentEntity を検索するメソッド
    Optional<StudentEntity> findByUsers(UsersEntity users);
}