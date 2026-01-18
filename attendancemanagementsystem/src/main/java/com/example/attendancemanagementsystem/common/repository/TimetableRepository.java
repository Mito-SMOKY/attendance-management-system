package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;

@Repository
public interface TimetableRepository extends JpaRepository<TimetableEntity, Integer> {

    // 学科と日付範囲で時間割を取得
    List<TimetableEntity> findByDepartmentAndDateBetween(DepartmentEntity department, LocalDate startDate, LocalDate endDate);

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

    //指定した教科・期間の時間割を日付順・時限順で取得
    List<TimetableEntity> findBySubjectIdAndDateBetweenOrderByDateAscSlotIdAsc(Integer subjectId, LocalDate startDate, LocalDate endDate);

    //学科IDと日付範囲を指定して時間割を取得
    List<TimetableEntity> findByDepartment_DepartmentIdAndDateBetweenOrderByDateAscSlotIdAsc(
            Integer departmentId, LocalDate startDate, LocalDate endDate);
    
    //指定した教科の最新の時間割エンティティを取得
    TimetableEntity findTopBySubjectIdOrderByDateDesc(Integer subjectId);

    //指定したユーザーIDの時間割エンティティを全て取得
    List<TimetableEntity> findByUserId(Integer userId);
    
    //指定したユーザIDの時間割エンティティの特定のコマを取得
    List<TimetableEntity> findByUserIdAndDate(Integer userId, LocalDate date);
    
    //指定したユーザIDの時間割エンティティの特定の日付・時限のエンティティを取得
    Optional<TimetableEntity> findByUserIdAndDateAndSlotId(Integer userId, LocalDate date, Integer slotId);

    // 指定した教員・日付・時間帯の簡易時間割情報を取得
    @Query("SELECT new map(" +
        "  sub.subjectId as subjectId, " +
        "  room.classroomId as classroomId, " +
        "  dept.departmentId as departmentId, " +
        "  ds.grade as targetGrade, " +       
        "  m.majorId as majorId, " +
        "  c.courseId as courseId " +
        ") " +
        "FROM TimetableEntity t " +
        "JOIN t.subject sub " +               
        "JOIN t.classroom room " +            
        "JOIN t.department dept " +           
        "JOIN dept.major m " +
        "LEFT JOIN m.course c " +
        "JOIN DepartmentSubject ds ON ds.department = dept AND ds.subject = sub " +
        "WHERE t.userId = :userId " +         
        "AND t.date = :date " +               
        "AND t.slotId = :slotId")            
    Map<String, Object> findSimpleTimetableData(
            @Param("userId") Integer userId, 
            @Param("date") LocalDate date, 
            @Param("slotId") Integer slotId
    );

    //教師IDに紐づく担当授業の情報を取得
    @Query("""
        SELECT DISTINCT 
            s.subjectId, 
            s.subjectName, 
            c.courseName, 
            ds.grade, 
            d.className
        FROM TimetableEntity t
        JOIN t.subject s
        JOIN t.department d
        JOIN d.major m
        JOIN m.course c
        JOIN DepartmentSubject ds ON ds.department = d AND ds.subject = s
        WHERE t.userId = :userId
        ORDER BY c.courseName, ds.grade, d.className
    """)
    List<Object[]> findTeacherSubjectsRaw(@Param("userId") Integer userId);

    //担当教員名を取得
    @Query(value = """
        SELECT u.Name 
        FROM timetable t
        JOIN users u ON t.UserID = u.UserID
        WHERE t.SubjectID = :subjectId 
        AND t.DepartmentID = :departmentId 
        LIMIT 1
        """, nativeQuery = true)
    List<String> findTeacherNameBySubjectAndClass(
        @Param("subjectId") Integer subjectId, 
        @Param("departmentId") Integer departmentId
    );

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
}