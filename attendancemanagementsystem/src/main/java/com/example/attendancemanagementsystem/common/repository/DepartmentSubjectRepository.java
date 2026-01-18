package com.example.attendancemanagementsystem.common.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.DepartmentSubject;
import com.example.attendancemanagementsystem.common.entity.DepartmentSubjectKey;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;

@Repository
public interface DepartmentSubjectRepository extends JpaRepository<DepartmentSubject, DepartmentSubjectKey> {

    // 学科IDにもとづく学年の取得
    @Query("SELECT DISTINCT d.grade FROM DepartmentSubject d WHERE d.id.departmentId = :departmentId ORDER BY d.grade ASC")
    List<Integer> findGradesByDepartmentId(@Param("departmentId") Integer departmentId);

    // 学科IDにもとづく科目の取得
    @Query("SELECT DISTINCT d.subject FROM DepartmentSubject d WHERE d.id.departmentId = :departmentId ORDER BY d.subject.subjectName ASC")
    List<SubjectEntity> findSubjectsByDepartmentId(@Param("departmentId") Integer departmentId);

    // 学科ID・学年にもとづく科目の取得
    @Query("SELECT DISTINCT ds.subject FROM DepartmentSubject ds " +
        "WHERE ds.id.departmentId = :departmentId " +
        "AND ds.grade = :grade " +
        "ORDER BY ds.subject.subjectName ASC")
    List<SubjectEntity> findSubjectsByDepartmentIdAndGrade(
            @Param("departmentId") Integer departmentId, 
            @Param("grade") Integer grade);
    
    //全教科・全クラスの組み合わせを取得
    @Query("SELECT DISTINCT " + 
        "  ds.department.departmentId, " + 
        "  ds.subject.subjectId, " + 
        "  ds.subject.subjectName, " +     
        "  ds.department.major.course.courseName, " + 
        "  ds.grade, " +                   
        "  ds.department.className " +     
        "FROM DepartmentSubject ds " +
        "ORDER BY ds.grade ASC, ds.department.className ASC, ds.subject.subjectName ASC")
    List<Object[]> findAllCurriculumRaw();
    
    //学科名とクラス名を取得する
    @Query(value = """
        SELECT m.MajorName, d.Class
        FROM department d
        JOIN major m ON d.MajorID = m.MajorID
        WHERE d.DepartmentID = :departmentId
        """, nativeQuery = true)
    List<Object[]> findCourseAndClass(@Param("departmentId") Integer departmentId);
}
