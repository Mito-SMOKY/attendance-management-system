package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.Datalist;

@Repository
public interface DatalistRepository extends JpaRepository<Datalist, Integer> {
    
    List<Datalist> findByCreatorId(Integer creatorId);

    // ★追加: 学生(students)と、そのユーザー情報(user)もまとめて一気に取得する専用メソッド
    @Query("SELECT d FROM Datalist d LEFT JOIN FETCH d.students s LEFT JOIN FETCH s.user WHERE d.dataListId = :id")
    Optional<Datalist> findByIdWithDetails(@Param("id") Integer id);
}