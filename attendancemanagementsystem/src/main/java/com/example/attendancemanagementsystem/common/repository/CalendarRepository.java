package com.example.attendancemanagementsystem.common.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// ntityパスをインポート
import com.example.attendancemanagementsystem.common.entity.CalendarEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;

@Repository
public interface CalendarRepository extends JpaRepository<CalendarEntity, Integer> {

    // ユーザーID と 日付の範囲 (StartDate から EndDate まで) で予定を検索
    List<CalendarEntity> findByUsersAndDateBetween(UsersEntity users, LocalDate startDate, LocalDate endDate);
}