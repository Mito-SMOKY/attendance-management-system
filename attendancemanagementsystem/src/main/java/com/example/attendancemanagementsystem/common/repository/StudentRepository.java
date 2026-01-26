package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Integer>, JpaSpecificationExecutor<StudentEntity> {

    // UsersEntity をもとに StudentEntity を検索するメソッド
    Optional<StudentEntity> findByUser(UsersEntity user);

    // 学科と学年で生徒を取得するメソッド
    @Query("SELECT s FROM StudentEntity s JOIN s.enrollments e " +
        "WHERE e.department.departmentId = :departmentId " +
        "AND e.grade = :grade " +
        "AND e.academicYear = :currentYear " +
        "AND e.isActive = true")
    List<StudentEntity> findByDepartmentAndGrade(
            @Param("departmentId") Integer departmentId, 
            @Param("grade") Integer grade, 
            @Param("currentYear") Integer currentYear);

    // キーワード検索とステータスIDリストでフィルタリング
    @Query("SELECT s FROM StudentEntity s " +
        "JOIN s.user u " + 
        "WHERE (u.name LIKE %:keyword% OR u.loginId LIKE %:keyword%) " +
        "AND s.studentStatusId IN :statusIds")
    Page<StudentEntity> searchByKeywordAndStatusIn(
            @Param("keyword") String keyword, 
            @Param("statusIds") List<Integer> statusIds, 
            Pageable pageable);


    // ステータスIDリストでフィルタリング
    @Query("SELECT s FROM StudentEntity s " +
        "WHERE s.studentStatusId IN :statusIds")
    Page<StudentEntity> findByStatusIn(
            @Param("statusIds") List<Integer> statusIds, 
            Pageable pageable);

    // キーワード検索のみ
    @Query("SELECT s FROM StudentEntity s " +
        "JOIN s.user u " + 
        "WHERE u.name LIKE %:keyword% OR u.loginId LIKE %:keyword%")
    Page<StudentEntity> searchByKeyword(
            @Param("keyword") String keyword, 
            Pageable pageable);
}