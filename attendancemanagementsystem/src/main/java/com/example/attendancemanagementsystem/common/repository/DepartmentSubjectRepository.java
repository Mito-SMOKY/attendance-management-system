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
    @Query("SELECT DISTINCT d.grade FROM DepartmentSubject d WHERE d.id.departmentId = :departmentId ORDER BY d.grade ASC")
    List<Integer> findGradesByDepartmentId(@Param("departmentId") Integer departmentId);

    @Query("SELECT DISTINCT d.subject FROM DepartmentSubject d WHERE d.id.departmentId = :departmentId ORDER BY d.subject.subjectName ASC")
    List<SubjectEntity> findSubjectsByDepartmentId(@Param("departmentId") Integer departmentId);
    
}