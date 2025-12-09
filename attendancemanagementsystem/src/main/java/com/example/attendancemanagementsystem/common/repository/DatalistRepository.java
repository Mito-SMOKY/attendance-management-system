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

    
    //d.creatorUser も一緒に取得するように "LEFT JOIN FETCH d.creatorUser" を追加
    @Query("SELECT d FROM Datalist d LEFT JOIN FETCH d.creatorUser LEFT JOIN FETCH d.students s LEFT JOIN FETCH s.user WHERE d.dataListId = :id")
    Optional<Datalist> findByIdWithDetails(@Param("id") Integer id);

    //全件取得（作成者情報付き）
    @Query("SELECT d FROM Datalist d LEFT JOIN FETCH d.creatorUser ORDER BY d.createdAt DESC")
    List<Datalist> findAllWithCreator();
}