package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;

@Repository
public interface EnrollmentsRepository extends JpaRepository<EnrollmentsEntity, Integer> {
    
    // 生徒に紐づく在籍情報（複数）を検索
    List<EnrollmentsEntity> findByStudent(StudentEntity student);

    @Query("SELECT e FROM EnrollmentsEntity e WHERE e.student.user = :user AND e.isActive = true")
    Optional<EnrollmentsEntity> findByUserAndIsActiveTrue(@Param("user") UsersEntity user);

    //学科IDと学年で在籍情報を検索するメソッド
    @Query("SELECT e FROM EnrollmentsEntity e WHERE e.department.departmentId = :deptId AND e.grade = :grade")
    List<EnrollmentsEntity> findByDepartmentIdAndGrade(@Param("deptId") Integer deptId, @Param("grade") Integer grade);
}