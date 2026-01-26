package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;

@Repository
public interface TimetableRepository extends JpaRepository<TimetableEntity, Integer> {

    // 学科と日付範囲で時間割を取得
    List<TimetableEntity> findByDepartmentAndDateBetween(DepartmentEntity department, LocalDate startDate, LocalDate endDate);

    // 指定された期間内に、授業が1コマでも登録されている学科を、重複なくリストアップする
    @Query("SELECT DISTINCT t.department FROM TimetableEntity t WHERE t.date BETWEEN :startDate AND :endDate")
    List<DepartmentEntity> findDistinctDepartmentByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 指定した学科・日付のデータを全て削除する（更新時のクリア用）
    void deleteByDepartment_DepartmentIdAndDate(Integer departmentId, LocalDate date);
    
    // 指定した学科・日付のデータを取得する（読み込み用）
    List<TimetableEntity> findByDepartment_DepartmentIdAndDateOrderBySlotIdAsc(Integer departmentId, LocalDate date);

    //1件ピンポイントで探すメソッド
    Optional<TimetableEntity> findByDepartment_DepartmentIdAndDateAndSlotId(Integer departmentId, LocalDate date, Integer slotId);

    // メソッド名の "_DepartmentID" は、TimetableEntity内の departmentフィールドの中にある DepartmentID を指します
    List<TimetableEntity> findByDateAndDepartment_DepartmentIdOrderBySlotId(LocalDate date, Integer departmentId);

    // 特定の学科に関連する科目の取得
    @Query("SELECT DISTINCT t FROM TimetableEntity t WHERE t.department.departmentId = :deptId")
    List<TimetableEntity> findDistinctSubjectsByDepartment(@Param("deptId") Integer deptId);

    // 特定の学科・科目で、今日までに実施された授業コマ数をカウント
    @Query("SELECT COUNT(t) FROM TimetableEntity t " +
        "WHERE t.department.departmentId = :deptId " +
        "AND t.subjectId = :subjectId " +
        "AND t.date <= CURRENT_DATE")
    int countTotalClassesBySubject(@Param("deptId") Integer deptId, @Param("subjectId") Integer subjectId);

    // 指定した教科・期間の時間割を日付順・時限順で取得
    List<TimetableEntity> findBySubjectIdAndDateBetweenOrderByDateAscSlotIdAsc(Integer subjectId, LocalDate startDate, LocalDate endDate);

    // 学科IDと日付範囲を指定して時間割を取得
    List<TimetableEntity> findByDepartment_DepartmentIdAndDateBetweenOrderByDateAscSlotIdAsc(
            Integer departmentId, LocalDate startDate, LocalDate endDate);
    
    // 指定した教科の最新の時間割エンティティを取得
    TimetableEntity findTopBySubjectIdOrderByDateDesc(Integer subjectId);

    // 指定したユーザーIDの時間割エンティティを全て取得
    List<TimetableEntity> findByUserId(Integer userId);
    
    // 指定したユーザIDの時間割エンティティの特定のコマを取得
    List<TimetableEntity> findByUserIdAndDate(Integer userId, LocalDate date);
    
    // 指定したユーザIDの時間割エンティティの特定の日付・時限のエンティティを取得
    Optional<TimetableEntity> findByUserIdAndDateAndSlotId(Integer userId, LocalDate date, Integer slotId);

    // 指定期間・指定学科のデータを一括削除
    @Modifying
    @Query("DELETE FROM TimetableEntity t WHERE t.department.departmentId = :deptId AND t.date BETWEEN :startDate AND :endDate")
    void deleteByDepartmentAndDateRange(@Param("deptId") Integer deptId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);


    // 参照画面用
    @Query("SELECT t FROM TimetableEntity t " +
        "LEFT JOIN FETCH t.subject " +
        "LEFT JOIN FETCH t.classroom " +
        "LEFT JOIN FETCH t.user " +
        "WHERE t.department.departmentId = :deptId " +
        "AND t.date BETWEEN :startDate AND :endDate " +
        "ORDER BY t.date ASC, t.slotId ASC")
    List<TimetableEntity> findForWeeklyView(
            @Param("deptId") Integer deptId, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate);

    // 指定した教員・日付・時間帯の簡易時間割情報を取得
    @Query("SELECT new map(" +
            "  sub.subjectId as subjectId, " +
            "  room.classroomId as classroomId, " +
            "  dept.departmentId as departmentId, " +
            "  t.grade as targetGrade, " +  
            "  m.majorId as majorId, " +
            "  c.courseId as courseId " +
            ") " +
            "FROM TimetableEntity t " +
            "JOIN t.subject sub " +
            "JOIN t.classroom room " +
            "JOIN t.department dept " +
            "JOIN dept.major m " +
            "LEFT JOIN m.course c " +
            "WHERE t.userId = :userId " +
            "AND t.date = :date " +
            "AND t.slotId = :slotId")
    Map<String, Object> findSimpleTimetableData(
            @Param("userId") Integer userId, 
            @Param("date") LocalDate date, 
            @Param("slotId") Integer slotId
    );
    
    //指定した科目IDの時間割エンティティ数をカウント
    int countBySubjectId(Integer subjectId);

    //曜日と教室を取得
    @Query(value = """
        SELECT t.Date, c.ClassroomName 
        FROM timetable t 
        LEFT JOIN classroom c ON t.ClassroomID = c.ClassroomID
        WHERE t.SubjectID = :subjectId 
        AND t.DepartmentID = :departmentId
        """, nativeQuery = true)
    List<Object[]> findScheduleAndRoom(
        @Param("subjectId") Integer subjectId, 
        @Param("departmentId") Integer departmentId
    );

    //登録されている時間割データの検索
    @Query("SELECT MIN(t.date), MAX(t.date), COUNT(t) " +
        "FROM TimetableEntity t " +
        "WHERE t.department.departmentId = :departmentId " +
        "AND t.date BETWEEN :startDate AND :endDate")
    List<Object[]> findOverlapSummary(
        @Param("departmentId") Integer departmentId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
}
    
