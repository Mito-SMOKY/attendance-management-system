package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import java.util.Optional; // ★追加

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.SubjectEntity;

@Repository
public interface SubjectRepository extends JpaRepository<SubjectEntity, Integer>, JpaSpecificationExecutor<SubjectEntity> {

    // ★追加: 名前で教科マスタを検索するメソッド
    Optional<SubjectEntity> findBySubjectName(String subjectName);

    // 教員のユーザIDにもとづく科目の取得
    @Query(value = "SELECT s.* FROM subject s " +
                "JOIN subjectfaculty sf ON s.SubjectID = sf.SubjectID " +
                "WHERE sf.UserID = :userId", nativeQuery = true)
    List<SubjectEntity> findSubjectsByTeacherId(@Param("userId") Integer userId);

    // コースIDにもとづく科目の取得
    @Query(value = "SELECT DISTINCT s.* FROM subject s " +
        "INNER JOIN departmentsubject ds ON s.SubjectID = ds.SubjectID " +
        "INNER JOIN department d ON ds.DepartmentID = d.DepartmentID " +
        "INNER JOIN major m ON d.MajorID = m.MajorID " +
        "WHERE m.CourseID = :courseId", nativeQuery = true)
    List<SubjectEntity> findByCourseId(@Param("courseId") Integer courseId);

    // 科目名と、それが対象とする「学科名」「学年」をセットで取得するクエリ
    @Query(value = """
        SELECT 
            s.SubjectName, 
            m.MajorName, 
            ds.Grade 
        FROM subject s
        JOIN departmentsubject ds ON s.SubjectID = ds.SubjectID
        JOIN department d ON ds.DepartmentID = d.DepartmentID
        JOIN major m ON d.MajorID = m.MajorID
        """, nativeQuery = true)
    List<Object[]> findAllSubjectsWithContext();

    // 学科IDに紐付く教科だけを検索する
    @Query(value = "SELECT s.* FROM subject s " +
                "INNER JOIN departmentsubject ds ON s.SubjectID = ds.SubjectID " +
                "WHERE ds.DepartmentID = :departmentId " +
                "AND ds.Grade = :grade " +  
                "ORDER BY s.SubjectName", nativeQuery = true)
    List<SubjectEntity> findByDepartmentIdAndGrade(
        @Param("departmentId") Integer departmentId, 
        @Param("grade") Integer grade
    );

    // 学科IDと学年、コースIDに紐付く教科を検索するクエリ
    @Query("SELECT ds.subject FROM DepartmentSubject ds " +
        "WHERE ds.department.departmentId = :departmentId " +
        "AND ds.grade = :grade " +
        "AND ds.department.major.course.courseId = :courseId")
    List<SubjectEntity> findSubjectsByDeptGradeAndCourse(
        @Param("departmentId") Integer deptId, 
        @Param("grade") Integer grade,
        @Param("courseId") Integer courseId
    );
}