package com.example.attendancemanagementsystem.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import com.example.attendancemanagementsystem.common.entity.MajorEntity;

@Repository
public interface MajorRepository extends JpaRepository<MajorEntity, Integer> {

    // コースIDにもとづく学科の取得
    @Query("SELECT m FROM MajorEntity m WHERE m.course.courseId = :courseId")
    List<MajorEntity> findByCourseId(@Param("courseId") Integer courseId);
}