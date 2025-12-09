package com.example.attendancemanagementsystem.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.DepartmentSubject;
import com.example.attendancemanagementsystem.common.entity.DepartmentSubjectKey;

@Repository
// ★修正: IDの型を Integer から DepartmentSubjectKey に変更
public interface DepartmentSubjectRepository extends JpaRepository<DepartmentSubject, DepartmentSubjectKey> {
    
}