package com.example.attendancemanagementsystem.common.repository;

import java.util.List; // List をインポート
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 正しいEntityパスをインポート
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;

@Repository
public interface EnrollmentsRepository extends JpaRepository<EnrollmentsEntity, Integer> {
    
    // 生徒に紐づく在籍情報（複数）を検索
    List<EnrollmentsEntity> findByStudent(StudentEntity student);
}