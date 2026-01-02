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
import com.example.attendancemanagementsystem.common.entity.StudentEntity;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Integer>{

       //生徒に紐づく出欠情報（複数）を検索
       List<AttendanceEntity> findByStudent(StudentEntity student);

       // セッションIDに紐づく全ての出席データを取得
       List<AttendanceEntity> findBySessionId(Integer sessionId);

       // 特定のセッションと生徒の出席データを取得（手動更新用）
       Optional<AttendanceEntity> findBySessionIdAndStudent_UserId(Integer sessionId, Integer userId);

       //同じ授業で既に登録済みかチェックする
       boolean existsBySessionIdAndStudent_UserId(Integer sessionId, Integer userId);

       //セッションの削除処理
       @Transactional 
       void deleteBySessionId(Integer sessionId);

       //生徒と日付範囲で出欠情報を検索(カレンダー表示用)
       @Query("SELECT a FROM AttendanceEntity a " +
              "WHERE a.student = :student " +
              "AND a.timeTable.date BETWEEN :startDate AND :endDate")
       List<AttendanceEntity> findByStudentAndDateRange(
              @Param("student") StudentEntity student,
              @Param("startDate") LocalDate startDate,
              @Param("endDate") LocalDate endDate);

       //カレンダー等の既存ロジック用
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " + 
              "AND a.timeTable.subjectId = :subjectId " +
              "AND a.status.statusId IN (1, 3, 4)") 
       int countAttendedClassesBySubject(@Param("userId") Integer userId, @Param("subjectId") Integer subjectId);
       
       //有効出席数カウント
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " +
              "AND a.timeTable.subjectId = :subjectId " +
              "AND a.status.statusId IN (1, 4)") 
       int countEffectiveAttendance(@Param("userId") Integer userId, @Param("subjectId") Integer subjectId);

       //出席停止数カウント
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " +
              "AND a.timeTable.subjectId = :subjectId " +
              "AND a.status.statusId = 6") 
       int countSuspensions(@Param("userId") Integer userId, @Param("subjectId") Integer subjectId);

       //詳細取得用
       @Query("SELECT a FROM AttendanceEntity a " +
              "WHERE a.student = :student " +
              "AND a.timeTable.date BETWEEN :startDate AND :endDate " +
              "AND a.timeTable.subjectId = :subjectId")
       List<AttendanceEntity> findByStudentAndDateRangeAndSubject(
              @Param("student") StudentEntity student, 
              @Param("startDate") LocalDate startDate, 
              @Param("endDate") LocalDate endDate, 
              @Param("subjectId") Integer subjectId);
       
       //集計用
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :studentId " +
              "AND a.timeTable.subjectId = :subjectId " +
              "AND a.status.id = :statusId")
       int countByStatusTotal(@Param("studentId") Integer studentId, 
              @Param("subjectId") Integer subjectId, 
              @Param("statusId") Integer statusId);

       //「指定した時間の直前に、同じ教室で終わった授業の出席データ」を探すメソッド
       @Query("SELECT a FROM AttendanceEntity a, SessionEntity s " +
              "WHERE a.sessionId = s.sessionId " +
              "AND a.student.userId = :userId " +
              "AND s.actualClassroomId = :classroomId " +
              "AND s.endTime BETWEEN :rangeStart AND :rangeEnd " +
              "AND (a.status.statusId = 1 OR a.status.statusId = 3)") 
       Optional<AttendanceEntity> findPreviousAttendanceInSameRoom(
              @Param("userId") Integer userId,
              @Param("classroomId") Integer classroomId,
              @Param("rangeStart") LocalDateTime rangeStart,
              @Param("rangeEnd") LocalDateTime rangeEnd
       );
}