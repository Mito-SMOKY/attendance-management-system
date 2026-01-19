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

    // --- ★今回追加した機能 ---
    // 教科IDで検索し、担当教員情報を含むエンティティリストを返す (JPAメソッド)
    List<SubjectFaculty> findBySubjectId(Integer subjectId);


    // --- 既存の機能 (変更なし) ---

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
}