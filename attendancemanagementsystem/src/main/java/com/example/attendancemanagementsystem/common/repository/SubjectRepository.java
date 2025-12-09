package com.example.attendancemanagementsystem.common.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.Subject;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    @Query("SELECT DISTINCT s FROM Subject s LEFT JOIN FETCH s.faculties sf LEFT JOIN FETCH sf.teacher")
    List<Subject> findAllWithDetails();
}