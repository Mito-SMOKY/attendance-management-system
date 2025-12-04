package com.example.attendancemanagementsystem.common.repository;

import com.example.attendancemanagementsystem.common.entity.CardsEntity;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


@Repository
public interface CardsRepository extends JpaRepository<CardsEntity, String> {

    // ユーザIDの検索
    Optional<CardsEntity> findByUserId(Integer userId);

    // カードIDの検索
    Optional<CardsEntity> findByCardId(String cardId);

    // 有効なカードIDの検索
    Optional<CardsEntity> findByUserIdAndIsActiveTrue(Integer userId);
}