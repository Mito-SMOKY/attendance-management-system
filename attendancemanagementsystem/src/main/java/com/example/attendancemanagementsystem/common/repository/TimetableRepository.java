package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Entityパスをインポート
import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;

@Repository
public interface TimetableRepository extends JpaRepository<TimetableEntity, Integer> {

    // 既存: 学科と日付の範囲 (startDate から endDate まで) で時間割を検索
    List<TimetableEntity> findByDepartmentAndDateBetween(DepartmentEntity department, LocalDate startDate, LocalDate endDate);

    // メソッド名の "_DepartmentID" は、TimetableEntity内の departmentフィールドの中にある DepartmentID を指します
    List<TimetableEntity> findByDateAndDepartment_DepartmentIdOrderBySlotId(LocalDate date, Integer departmentId);

}