package com.example.attendancemanagementsystem.common.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;


@Repository
public interface SubjectRepository extends JpaRepository<SubjectEntity, Integer> {

    // subjectfaculty テーブルを結合して検索します
    @Query(value = "SELECT s.* FROM subject s " +
                "JOIN subjectfaculty sf ON s.SubjectID = sf.SubjectID " +
                "WHERE sf.UserID = :userId", nativeQuery = true)
    List<SubjectEntity> findSubjectsByTeacherId(@Param("userId") Integer userId);

}