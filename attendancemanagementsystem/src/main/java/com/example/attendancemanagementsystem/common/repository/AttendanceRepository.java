package com.example.attendancemanagementsystem.common.repository;

import java.util.List; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Entityパスをインポート
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Integer>{

    // 生徒に紐づく出欠情報（複数）を検索
    List<AttendanceEntity> findByStudent(StudentEntity student);

}
