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

    // --- ★修正: JPQLに変更して検索精度を向上 ---
    // DepartmentSubjectエンティティを経由して、条件に合う教科IDを取得
    @Query("SELECT ds.subject.subjectId FROM DepartmentSubject ds WHERE ds.department.departmentId = :deptId AND ds.grade = :grade AND ds.subject.subjectName = :subjectName")
    List<Integer> findSubjectIdsByClassAndSubjectName(@Param("deptId") Integer deptId, @Param("grade") Integer grade, @Param("subjectName") String subjectName);

    // --- 既存機能 ---
    
    // アプリ起動エラー回避用
    @Query(value = "SELECT * FROM subjectfaculty WHERE SubjectID = :subjectId", nativeQuery = true)
    List<SubjectFaculty> findBySubjectId(@Param("subjectId") Integer subjectId);

    // 削除チェック用
    @Query(value = "SELECT COUNT(*) FROM timetable WHERE SubjectID = :subjectId", nativeQuery = true)
    int countTimeTableUsage(@Param("subjectId") Integer subjectId);

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