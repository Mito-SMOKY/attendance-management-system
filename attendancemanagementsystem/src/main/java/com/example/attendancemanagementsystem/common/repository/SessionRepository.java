package com.example.attendancemanagementsystem.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.SessionEntity;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, Integer>{
    //実施中の授業を探すメソッド
    SessionEntity findFirstBySessionStatusOrderBySessionIdDesc(Integer sessionStatus);
}
