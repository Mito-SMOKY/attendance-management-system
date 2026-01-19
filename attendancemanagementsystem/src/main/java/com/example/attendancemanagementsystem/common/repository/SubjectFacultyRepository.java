package com.example.attendancemanagementsystem.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.SubjectEntity;

@Repository
public interface SubjectFacultyRepository extends JpaRepository<SubjectEntity, Integer> {

    // 教科IDに紐づいている教師のIDを取得 (1件のみ取得)
    @Query(value = "SELECT UserID FROM subjectfaculty WHERE SubjectID = :subjectId LIMIT 1", nativeQuery = true)
    Integer findTeacherIdBySubjectId(@Param("subjectId") Integer subjectId);

    // 教科と教師の紐づけを保存 (INSERT)
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO subjectfaculty (SubjectID, UserID) VALUES (:subjectId, :userId)", nativeQuery = true)
    void insertSubjectFaculty(@Param("subjectId") Integer subjectId, @Param("userId") Integer userId);

    // 特定の教科IDの紐づけを全て削除 (DELETE)
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM subjectfaculty WHERE SubjectID = :subjectId", nativeQuery = true)
    void deleteBySubjectId(@Param("subjectId") Integer subjectId);

    //教師に紐づいてる教科・学科学年・クラスを取得
    @Query(value = """
        SELECT DISTINCT
            s.SubjectID, 
            s.SubjectName, 
            ds.DepartmentID, 
            ds.Grade,
            d.Class   
        FROM subjectfaculty sf
        JOIN subject s ON sf.SubjectID = s.SubjectID
        JOIN departmentsubject ds ON s.SubjectID = ds.SubjectID
        JOIN department d ON ds.DepartmentID = d.DepartmentID 
        WHERE sf.UserID = :userId
        ORDER BY ds.DepartmentID, ds.Grade, d.Class, s.SubjectID
    """, nativeQuery = true)
    List<Object[]> findSubjectDetailsByTeacherId(@Param("userId") Integer userId);
}
