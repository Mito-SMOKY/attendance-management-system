package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.RequestEntity;

@Repository
public interface RequestRepository extends JpaRepository<RequestEntity, Integer> {

    // 特定のユーザーが申請したデータを、作成日が新しい順に取得する
    List<RequestEntity> findByRequesterUserIdOrderByCreatedAtDesc(Integer requesterUserId);

    //ステータス指定で取得 (例: 申請中のものだけ取得)
    List<RequestEntity> findByStatus(Integer status);

    // 特定のユーザーが承認者である申請データを、作成日が新しい順に取得する
    List<RequestEntity> findByApproverIdOrderByCreatedAtDesc(Integer approverId);

    // 特定のユーザーが申請者であり、かつ特定のステータスの申請データが存在するか確認する
    boolean existsByRequesterUserIdAndStatus(Integer requesterUserId, Integer status);

    List<RequestEntity> findByRequesterUserIdInOrderByCreatedAtDesc(Collection<Integer> requesterUserIds);

    List<RequestEntity> findByRequesterUserIdNotInOrderByCreatedAtDesc(Collection<Integer> excludeUserIds);

    List<RequestEntity> findByStatusAndRequesterUserIdInOrderByCreatedAtAsc(Integer status, Collection<Integer> requesterUserIds);
}