package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // ★追加
import org.springframework.stereotype.Repository;

import com.example.attendancemanagementsystem.common.entity.AdministratorEntity;

@Repository
// ★修正: JpaSpecificationExecutor<AdministratorEntity> を追加継承
public interface AdministratorRepository extends JpaRepository<AdministratorEntity, Integer>, JpaSpecificationExecutor<AdministratorEntity> {
    
    // ★削除: 以前追加した findAllWithUser() はもう使いません（Specificationで代用するため）
    // @Query("SELECT a FROM AdministratorEntity a JOIN FETCH a.user ORDER BY a.userId ASC")
    // List<AdministratorEntity> findAllWithUser();

    // UserIDを主キーとしているため、標準の findById で検索可能です

    List<AdministratorEntity> findByAdminLevelId(Integer adminLevelId);
}
