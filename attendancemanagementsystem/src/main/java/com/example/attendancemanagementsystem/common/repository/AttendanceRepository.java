package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDate;
import java.util.List; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// Entityパスをインポート
import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Integer>{

    // 生徒に紐づく出欠情報（複数）を検索
    List<AttendanceEntity> findByStudent(StudentEntity student);


    // 生徒と日付範囲で出欠情報を検索(カレンダー表示用)
    @Query("SELECT a FROM AttendanceEntity a " +
           "WHERE a.student = :student " +
           "AND a.timeTable.date BETWEEN :startDate AND :endDate")
    List<AttendanceEntity> findByStudentAndDateRange(
            @Param("student") StudentEntity student,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


}
