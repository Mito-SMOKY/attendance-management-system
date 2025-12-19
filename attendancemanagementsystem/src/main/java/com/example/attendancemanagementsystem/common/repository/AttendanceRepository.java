package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDate;
import java.util.List; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Integer>{

       //生徒に紐づく出欠情報（複数）を検索
       List<AttendanceEntity> findByStudent(StudentEntity student);

       //生徒と日付範囲で出欠情報を検索(カレンダー表示用)
       @Query("SELECT a FROM AttendanceEntity a " +
              "WHERE a.student = :student " +
              "AND a.timeTable.date BETWEEN :startDate AND :endDate")
       List<AttendanceEntity> findByStudentAndDateRange(
              @Param("student") StudentEntity student,
              @Param("startDate") LocalDate startDate,
              @Param("endDate") LocalDate endDate);

       //カレンダー等の既存ロジック用（現状維持）
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " + 
              "AND a.timeTable.subjectId = :subjectId " +
              "AND a.status.statusId IN (1, 3, 4)") 
       int countAttendedClassesBySubject(@Param("userId") Integer userId, @Param("subjectId") Integer subjectId);
       
       //有効出席数カウント (出席+公欠)
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " +
              "AND a.timeTable.subjectId = :subjectId " +
           "AND a.status.statusId IN (1, 4)") // 1:出席, 4:公欠
       int countEffectiveAttendance(@Param("userId") Integer userId, @Param("subjectId") Integer subjectId);

       //出席停止数カウント (分母から引くため)
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :userId " +
              "AND a.timeTable.subjectId = :subjectId " +
              "AND a.status.statusId = 6") // 6:出席停止
       int countSuspensions(@Param("userId") Integer userId, @Param("subjectId") Integer subjectId);

       //指定した生徒・期間・教科の出席データを取得
       //(これが無いと SubjectAttendanceDetailService でエラーになります)
       @Query("SELECT a FROM AttendanceEntity a " +
              "WHERE a.student = :student " +
              "AND a.timeTable.date BETWEEN :startDate AND :endDate " +
              "AND a.timeTable.subjectId = :subjectId")
       List<AttendanceEntity> findByStudentAndDateRangeAndSubject(
              @Param("student") StudentEntity student, 
              @Param("startDate") LocalDate startDate, 
              @Param("endDate") LocalDate endDate, 
              @Param("subjectId") Integer subjectId);
       
       //指定した生徒・教科・ステータスの出席数をカウント(SubjectAttendanceServiceの全期間計算用)
       @Query("SELECT COUNT(a) FROM AttendanceEntity a " +
              "WHERE a.student.userId = :studentId " +
              "AND a.timeTable.subjectId = :subjectId " +
              "AND a.status.id = :statusId")
       int countByStatusTotal(@Param("studentId") Integer studentId, 
              @Param("subjectId") Integer subjectId, 
              @Param("statusId") Integer statusId);

}