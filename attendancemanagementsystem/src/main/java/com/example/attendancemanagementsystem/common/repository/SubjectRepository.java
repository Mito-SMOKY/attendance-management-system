package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import java.util.Optional; // ★追加

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.SubjectEntity;

@Repository
public interface SubjectRepository extends JpaRepository<SubjectEntity, Integer>, JpaSpecificationExecutor<SubjectEntity> {

    // ★追加: 名前で教科マスタを検索するメソッド
    Optional<SubjectEntity> findBySubjectName(String subjectName);

    // 教員のユーザIDにもとづく科目の取得
    @Query(value = "SELECT s.* FROM subject s " +
                "JOIN subjectfaculty sf ON s.SubjectID = sf.SubjectID " +
                "WHERE sf.UserID = :userId", nativeQuery = true)
    List<SubjectEntity> findSubjectsByTeacherId(@Param("userId") Integer userId);

    // コースIDにもとづく科目の取得
    @Query(value = "SELECT DISTINCT s.* FROM subject s " +
        "INNER JOIN departmentsubject ds ON s.SubjectID = ds.SubjectID " +
        "INNER JOIN department d ON ds.DepartmentID = d.DepartmentID " +
        "INNER JOIN major m ON d.MajorID = m.MajorID " +
        "WHERE m.CourseID = :courseId", nativeQuery = true)
    List<SubjectEntity> findByCourseId(@Param("courseId") Integer courseId);

}