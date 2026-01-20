package com.example.attendancemanagementsystem.common.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Integer> {
    
    // コースIDにもとづくクラス名の取得
    @Query("SELECT d.className FROM DepartmentEntity d WHERE d.major.course.courseId = :courseId ORDER BY d.className")
    List<String> findClassNamesByCourseId(@Param("courseId") Integer courseId);
}
