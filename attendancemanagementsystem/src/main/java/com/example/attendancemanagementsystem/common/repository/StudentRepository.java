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

    // 全ての生徒をページング付きで取得するメソッド（削除済み除外を追加）
    @Override
    @Query("SELECT s FROM StudentEntity s WHERE s.user.deleteFlag = false")
    Page<StudentEntity> findAll(Pageable pageable);

    // 学科と学年で生徒を取得するメソッド（削除済み除外を追加）
    @Query("SELECT s FROM StudentEntity s JOIN s.enrollments e " +
        "WHERE e.department.departmentId = :departmentId " +
        "AND e.grade = :grade " +
        "AND e.academicYear = :currentYear " +
        "AND e.isActive = true " +
        "AND s.user.deleteFlag = false")
    List<StudentEntity> findByDepartmentAndGrade(
            @Param("departmentId") Integer departmentId, 
            @Param("grade") Integer grade, 
            @Param("currentYear") Integer currentYear);

    // キーワード検索とステータスIDリストでフィルタリング（削除済み除外を追加）
    @Query("SELECT s FROM StudentEntity s " +
        "JOIN s.user u " + 
        "WHERE (u.name LIKE %:keyword% OR u.loginId LIKE %:keyword%) " +
        "AND s.studentStatusId IN :statusIds " +
        "AND u.deleteFlag = false")
    Page<StudentEntity> searchByKeywordAndStatusIn(
            @Param("keyword") String keyword, 
            @Param("statusIds") List<Integer> statusIds, 
            Pageable pageable);


    // ステータスIDリストでフィルタリング（削除済み除外を追加）
    @Query("SELECT s FROM StudentEntity s " +
        "WHERE s.studentStatusId IN :statusIds " +
        "AND s.user.deleteFlag = false")
    Page<StudentEntity> findByStatusIn(
            @Param("statusIds") List<Integer> statusIds, 
            Pageable pageable);

    // キーワード検索のみ（削除済み除外を追加）
    @Query("SELECT s FROM StudentEntity s " +
        "JOIN s.user u " + 
        "WHERE (u.name LIKE %:keyword% OR u.loginId LIKE %:keyword%) " +
        "AND u.deleteFlag = false")
    Page<StudentEntity> searchByKeyword(
            @Param("keyword") String keyword, 
            Pageable pageable);
}