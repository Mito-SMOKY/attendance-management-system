package com.example.attendancemanagementsystem.common.repository;

import com.example.attendancemanagementsystem.common.entity.ClassroomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClassroomRepository extends JpaRepository<ClassroomEntity,Integer> {
    // MACアドレスで教室を検索
    Optional<ClassroomEntity> findByMacAddress(String macAddress);
}