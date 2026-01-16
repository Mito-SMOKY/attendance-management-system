package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, Integer>, JpaSpecificationExecutor<SessionEntity> {

    // 実施中の授業を探すメソッド (単一取得用)
    SessionEntity findFirstBySessionFlagOrderBySessionIdDesc(boolean sessionFlag);

    //教員のユーザIDにもとづく実施中セッションの取得 (複数返却可)
    @Query("SELECT s FROM SessionEntity s WHERE s.userId = :userId AND s.sessionFlag = false")
    List<SessionEntity> findActiveSessionsByUserId(@Param("userId") Integer userId);

    // 指定した教員・日付・時間帯のセッションを探すメソッド
    @Query("SELECT s FROM SessionEntity s WHERE s.userId = :userId AND s.sessionDate = :date AND s.timeSlot.slotId = :slotId ORDER BY s.sessionId DESC")
    List<SessionEntity> findByUserIdAndDateAndSlotId(
        @Param("userId") Integer userId, 
        @Param("date") LocalDate date, 
        @Param("slotId") Integer slotId
    );

    // 前回の授業状態を確認するため等に使用
    List<SessionEntity> findByDepartmentAndTargetGradeAndSubjectAndSessionDate(
        DepartmentEntity department, 
        Integer targetGrade, 
        SubjectEntity subject, 
        LocalDate sessionDate
    );

    // 指定期間内のセッションを取得
    @Query("SELECT s FROM SessionEntity s WHERE s.sessionDate BETWEEN :startDate AND :endDate ORDER BY s.sessionDate, s.timeSlot.slotId")
    List<SessionEntity> findBySessionDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 特定の日付と時間帯に基づくセッションの取得
    SessionEntity findBySessionDateAndTimeSlotSlotId(LocalDate sessionDate, Integer slotId);

    // 指定した教員・日付のセッションをすべて取得 
    @Query("SELECT s FROM SessionEntity s WHERE s.userId = :userId AND s.sessionDate = :date ORDER BY s.timeSlot.slotId ASC")
    List<SessionEntity> findByUserIdAndDate(
        @Param("userId") Integer userId, 
        @Param("date") LocalDate date
    );
}