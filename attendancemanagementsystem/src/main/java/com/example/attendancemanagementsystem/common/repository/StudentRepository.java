package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Integer> {

    // UsersEntity をもとに StudentEntity を検索するメソッド
    Optional<StudentEntity> findByUsers(UsersEntity users);

    @Query("SELECT s FROM StudentEntity s JOIN s.enrollments e " +
        "WHERE e.department.departmentId = :departmentId " +
        "AND e.grade = :grade " +
        "AND e.academicYear = :currentYear " +
        "AND e.isActive = true")
    List<StudentEntity> findByDepartmentAndGrade(
            @Param("departmentId") Integer departmentId, 
            @Param("grade") Integer grade, 
            @Param("currentYear") Integer currentYear);
}