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

    // ユーザーとアクティブ状態で検索
    @Query("SELECT e FROM EnrollmentsEntity e WHERE e.student.users = :user AND e.isActive = true")
    Optional<EnrollmentsEntity> findByUserAndIsActiveTrue(@Param("user") UsersEntity user);

    // 学科IDで検索
    List<EnrollmentsEntity> findByDepartment_DepartmentId(Integer departmentId);

    // 学科IDと学年で検索 
    @Query("SELECT e FROM EnrollmentsEntity e WHERE e.department.departmentId = :deptId AND e.grade = :grade")
    List<EnrollmentsEntity> findByDepartmentIdAndGrade(@Param("deptId") Integer deptId, @Param("grade") Integer grade);

    // コースIDと学年で検索 
    @Query(value = "SELECT e.* FROM enrollments e " +
        "INNER JOIN department d ON e.DepartmentID = d.DepartmentID " +
        "INNER JOIN major m ON d.MajorID = m.MajorID " +
        "WHERE m.CourseID = :courseId " +
        "AND e.Grade = :grade " +
        "AND e.IsActive = 1", nativeQuery = true)
    List<EnrollmentsEntity> findByCourseIdAndGrade(@Param("courseId") Integer courseId, @Param("grade") Integer grade);

    // コースIDから学年リスト (★追加: これがないと学年プルダウンが出ません)
    @Query(value = "SELECT DISTINCT e.Grade FROM enrollments e " +
        "INNER JOIN department d ON e.DepartmentID = d.DepartmentID " +
        "INNER JOIN major m ON d.MajorID = m.MajorID " +
        "WHERE m.CourseID = :courseId " +
        "AND e.IsActive = 1 " + 
        "ORDER BY e.Grade", nativeQuery = true)
    List<Integer> findDistinctGradesByCourseId(@Param("courseId") Integer courseId);

    // コースIDに基づく在籍学年・クラスID・クラス名の取得 
    @Query(value = "SELECT DISTINCT d.DepartmentID, d.Class FROM enrollments e " +
        "INNER JOIN department d ON e.DepartmentID = d.DepartmentID " +
        "INNER JOIN major m ON d.MajorID = m.MajorID " +
        "WHERE m.CourseID = :courseId " +
        "AND e.Grade = :grade " +
        "AND e.IsActive = 1 " +
        "ORDER BY d.Class", nativeQuery = true)
    List<Object[]> findDistinctDepartmentIdAndClass(
        @Param("courseId") Integer courseId, 
        @Param("grade") Integer grade
    );

    // コースIDと学年に基づく在籍クラス名の取得 (一応残しておきます)
    @Query(value = "SELECT DISTINCT d.Class FROM enrollments e " +
        "INNER JOIN department d ON e.DepartmentID = d.DepartmentID " +
        "INNER JOIN major m ON d.MajorID = m.MajorID " +
        "WHERE m.CourseID = :courseId " +
        "AND e.Grade = :grade " +
        "AND e.IsActive = 1 " +
        "ORDER BY d.Class", nativeQuery = true)
    List<String> findDistinctClassesByCourseIdAndGrade(
        @Param("courseId") Integer courseId, 
        @Param("grade") Integer grade
    );

    //指定したクラス・学年に在籍する学生の名前を取得
    @Query(value = """
        SELECT u.Name 
        FROM enrollments e
        JOIN users u ON e.UserID = u.UserID
        WHERE e.DepartmentID = :departmentId 
        AND e.Grade = :grade 
        AND e.IsActive = 1
        ORDER BY u.Name ASC
        """, nativeQuery = true)
    List<String> findStudentNamesByClass(
        @Param("departmentId") Integer departmentId, 
        @Param("grade") Integer grade
    );

    //クラスの生徒IDと名前を取得する
    @Query("SELECT s.userId, u.name " +
        "FROM EnrollmentsEntity e " +
        "JOIN e.student s " +
        "JOIN s.users u " +
        "WHERE e.department.departmentId = :departmentId " +
        "AND e.grade = :grade " +
        "ORDER BY s.userId ASC")
    List<Object[]> findStudentIdAndNamesByClass(
            @Param("departmentId") Integer departmentId, 
            @Param("grade") Integer grade);
}