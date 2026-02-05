package com.example.attendancemanagementsystem.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.SubjectFaculty;
import com.example.attendancemanagementsystem.common.entity.SubjectFacultyId;

@Repository
public interface SubjectFacultyRepository extends JpaRepository<SubjectFaculty, SubjectFacultyId> {

    // --- ★修正: 重複チェック用のSQLを追加 ---
    // 指定された「クラス・学年・教科名」を持つ教科IDのリストを返します
    @Query(value = "SELECT s.SubjectID FROM departmentsubject ds JOIN subject s ON ds.SubjectID = s.SubjectID WHERE ds.DepartmentID = :deptId AND ds.Grade = :grade AND s.SubjectName = :subjectName", nativeQuery = true)
    List<Integer> findSubjectIdsByClassAndSubjectName(@Param("deptId") Integer deptId, @Param("grade") Integer grade, @Param("subjectName") String subjectName);

    // --- 既存: アプリ起動エラー回避 ---
    @Query(value = "SELECT * FROM subjectfaculty WHERE SubjectID = :subjectId", nativeQuery = true)
    List<SubjectFaculty> findBySubjectId(@Param("subjectId") Integer subjectId);

    // --- 既存: 削除チェック ---
    @Query(value = "SELECT COUNT(*) FROM timetable WHERE SubjectID = :subjectId", nativeQuery = true)
    int countTimeTableUsage(@Param("subjectId") Integer subjectId);

    // --- 以下、既存機能 ---
    @Query(value = "SELECT UserID FROM subjectfaculty WHERE SubjectID = :subjectId LIMIT 1", nativeQuery = true)
    Integer findTeacherIdBySubjectId(@Param("subjectId") Integer subjectId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO subjectfaculty (SubjectID, UserID) VALUES (:subjectId, :userId)", nativeQuery = true)
    void insertSubjectFaculty(@Param("subjectId") Integer subjectId, @Param("userId") Integer userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM subjectfaculty WHERE SubjectID = :subjectId", nativeQuery = true)
    void deleteBySubjectId(@Param("subjectId") Integer subjectId);

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

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM subjectfaculty WHERE SubjectID = :subjectId AND UserID = :userId", nativeQuery = true)
    void deleteBySubjectIdAndUserId(@Param("subjectId") Integer subjectId, @Param("userId") Integer userId);
    
    @Query(value = "SELECT UserID FROM subjectfaculty WHERE SubjectID = :subjectId", nativeQuery = true)
    List<Integer> findTeacherIdsBySubjectId(@Param("subjectId") Integer subjectId);
}