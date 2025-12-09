package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
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

    //指定した教科・期間の時間割を日付順・時限順で取得
    List<TimetableEntity> findBySubjectIdAndDateBetweenOrderByDateAscSlotIdAsc(Integer subjectId, LocalDate startDate, LocalDate endDate);

    //学科IDと日付範囲を指定して時間割を取得
    List<TimetableEntity> findByDepartment_DepartmentIdAndDateBetweenOrderByDateAscSlotIdAsc(
            Integer departmentId, LocalDate startDate, LocalDate endDate);

}