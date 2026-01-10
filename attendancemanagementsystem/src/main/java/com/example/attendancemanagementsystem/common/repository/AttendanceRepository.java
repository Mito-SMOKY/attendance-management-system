package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Integer> {

       // メソッド名によるクエリ生成
       List<AttendanceEntity> findByStudent_UserId(Integer userId);

       // セッションIDにもとづく出席情報の取得
       List<AttendanceEntity> findBySessionId(Integer sessionId);

       // セッションIDと学生のユーザIDにもとづく出席情報の取得
       Optional<AttendanceEntity> findBySessionIdAndStudent_UserId(Integer sessionId, Integer userId);

       // 出席情報の存在確認
       boolean existsBySessionIdAndStudent_UserId(Integer sessionId, Integer userId);

       @Transactional 
       void deleteBySessionId(Integer sessionId);

       // 日付範囲検索
       @Query("SELECT a FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " +
              "AND a.session.sessionDate BETWEEN :startDate AND :endDate")
       List<AttendanceEntity> findByStudentAndDateRange(
              @Param("userId") Integer userId, 
              @Param("startDate") LocalDate startDate,
              @Param("endDate") LocalDate endDate);

       // 科目別出席カウント (出席、公欠、遅刻)
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " + 
              "AND a.session.subject.subjectId = :subjectId " + 
              "AND a.status.statusId IN (1, 3, 4)") 
       int countAttendedClassesBySubject(@Param("userId") Integer userId, @Param("subjectId") Integer subjectId);

       // 有効出席カウント (出席、公欠)
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " +
              "AND a.session.subject.subjectId = :subjectId " +
              "AND a.status.statusId IN (1, 4)") 
       int countEffectiveAttendance(@Param("userId") Integer userId, @Param("subjectId") Integer subjectId);

       // 出席停止カウント
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " +
              "AND a.session.subject.subjectId = :subjectId " +
              "AND a.status.statusId = 6") 
       int countSuspensions(@Param("userId") Integer userId, @Param("subjectId") Integer subjectId);

       // 詳細取得 (カレンダー等)
       @Query("SELECT a FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " +
              "AND a.session.sessionDate BETWEEN :startDate AND :endDate " +
              "AND a.session.subject.subjectId = :subjectId")
       List<AttendanceEntity> findByStudentAndDateRangeAndSubject(
              @Param("userId") Integer userId,
              @Param("startDate") LocalDate startDate, 
              @Param("endDate") LocalDate endDate, 
              @Param("subjectId") Integer subjectId);

       // ステータス別集計
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :studentId " +
              "AND a.session.subject.subjectId = :subjectId " +
              "AND a.status.statusId = :statusId")
       int countByStatusTotal(@Param("studentId") Integer studentId, 
              @Param("subjectId") Integer subjectId, 
              @Param("statusId") Integer statusId);

       // 連続授業判定
       @Query("SELECT a FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " +
              "AND a.session.classroom.classroomId = :classroomId " +
              "AND a.session.endTime BETWEEN :start AND :end")
       Optional<AttendanceEntity> findContinuousAttendance(
              @Param("userId") Integer userId, 
              @Param("classroomId") Integer classroomId, 
              @Param("start") LocalDateTime start, 
              @Param("end") LocalDateTime end
       );

       // 科目別出席情報取得（カレンダー表示用）
       @Query("SELECT a FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " +
              "AND a.session.subject.subjectId = :subjectId " +
              "ORDER BY a.session.sessionDate ASC, a.session.startTime ASC")
       List<AttendanceEntity> findByStudentAndSubject(
              @Param("userId") Integer userId,
              @Param("subjectId") Integer subjectId);
}