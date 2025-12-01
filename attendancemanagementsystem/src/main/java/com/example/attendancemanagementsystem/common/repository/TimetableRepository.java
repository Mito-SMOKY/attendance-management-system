package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// Entityパスをインポート
import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;

@Repository
public interface TimetableRepository extends JpaRepository<TimetableEntity, Integer> {

    // 既存: 学科と日付の範囲 (startDate から endDate まで) で時間割を検索
    List<TimetableEntity> findByDepartmentAndDateBetween(DepartmentEntity department, LocalDate startDate, LocalDate endDate);

    // メソッド名の "_DepartmentID" は、TimetableEntity内の departmentフィールドの中にある DepartmentID を指します
    List<TimetableEntity> findByDateAndDepartment_DepartmentIdOrderBySlotId(LocalDate date, Integer departmentId);

    // ① その学科の「科目」一覧を取得
    @Query("SELECT DISTINCT t FROM TimetableEntity t WHERE t.department.departmentId = :deptId")
    List<TimetableEntity> findDistinctSubjectsByDepartment(@Param("deptId") Integer deptId);

    // ② 特定の学科・科目で、今日までに実施された授業コマ数をカウント
    @Query("SELECT COUNT(t) FROM TimetableEntity t " +
           "WHERE t.department.departmentId = :deptId " +
           "AND t.subjectId = :subjectId " +
           "AND t.date <= CURRENT_DATE")
    int countTotalClassesBySubject(@Param("deptId") Integer deptId, @Param("subjectId") Integer subjectId);

}