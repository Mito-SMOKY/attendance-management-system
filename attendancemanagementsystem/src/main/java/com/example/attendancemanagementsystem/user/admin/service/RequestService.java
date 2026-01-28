package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service("adminRequestService")
public class RequestService {

    @Autowired
    private StudentRepository studentRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    // ステータスID定義
    private static final List<Integer> ACTIVE_STATUS_IDS = Arrays.asList(1, 3, 6); // 在籍, 停学, 休学
    private static final List<Integer> INACTIVE_STATUS_IDS = Arrays.asList(2, 4, 5); // 退学, 卒業, 除籍
    
    // 申請種別ID定義 (DBのrequest_typeテーブルに準拠)
    private static final int TYPE_DELETE_ACCOUNT = 2;
    private static final int TYPE_CHANGE_STATUS = 3; // 仮
    private static final int TYPE_CHANGE_COURSE = 4; // 仮

    // 申請対象生徒の検索・取得 (isPendingフラグ付き)
    public Page<Map<String, Object>> searchStudentsForSelection(String mode, String keyword, Pageable pageable) {
        // 1. 生徒データの取得 (既存ロジック)
        Page<StudentEntity> studentPage = getTargetStudents(mode, keyword, pageable);
        
        // 2. 表示用Mapに変換
        List<Map<String, Object>> displayList = convertToDisplayData(studentPage.getContent());
        
        // 3. 申請中の生徒IDを取得
        Set<Integer> pendingIds = getPendingStudentIds(mode);
        
        // 4. isPendingフラグを注入
        for (Map<String, Object> map : displayList) {
            Integer userId = (Integer) map.get("userId");
            if (pendingIds.contains(userId)) {
                map.put("isPending", true);
            } else {
                map.put("isPending", false);
            }
        }
        
        return new PageImpl<>(displayList, pageable, studentPage.getTotalElements());
    }

    // 申請対象生徒の取得ロジック
    public Page<StudentEntity> getTargetStudents(String mode, String keyword, Pageable pageable) {
        List<Integer> targetStatusIds = null;

        if ("delete".equals(mode)) {
            targetStatusIds = INACTIVE_STATUS_IDS;
        } else if ("course".equals(mode)) {
            targetStatusIds = ACTIVE_STATUS_IDS;
        }
        // status変更の場合は全ステータス対象(nullのまま)とするなど、仕様に合わせて調整

        if (keyword != null && !keyword.isEmpty()) {
            if (targetStatusIds != null) {
                return studentRepository.searchByKeywordAndStatusIn(keyword, targetStatusIds, pageable);
            } else {
                return studentRepository.searchByKeyword(keyword, pageable);
            }
        } else {
            if (targetStatusIds != null) {
                return studentRepository.findByStatusIn(targetStatusIds, pageable);
            } else {
                return studentRepository.findAll(pageable);
            }
        }
    }

    // IDリストから生徒エンティティリストを取得
    public List<StudentEntity> getStudentsByIds(List<Integer> ids) {
        return studentRepository.findAllById(ids);
    }

    // 申請中の生徒IDセットを取得
    @SuppressWarnings("unchecked")
    private Set<Integer> getPendingStudentIds(String mode) {
        int requestTypeId = 0;
        
        // モードから申請種別IDを判定
        if ("delete".equals(mode)) {
            requestTypeId = TYPE_DELETE_ACCOUNT;
        } else if ("status".equals(mode)) {
            requestTypeId = TYPE_CHANGE_STATUS;
        } else if ("course".equals(mode)) {
            requestTypeId = TYPE_CHANGE_COURSE;
        }
        
        if (requestTypeId == 0) {
            return Collections.emptySet();
        }

        // Status=1(承認待ち) かつ RequestTypeIDが一致する申請に含まれる生徒IDを取得
        String sql = """
            SELECT DISTINCT rt.UserID
            FROM request r
            JOIN requesttargetuser rt ON r.RequestID = rt.RequestID
            WHERE r.RequestTypeID = :typeId
            AND r.Status = 1
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("typeId", requestTypeId);
        
        List<Integer> resultList = query.getResultList();
        return new HashSet<>(resultList);
    }

    // 生徒エンティティリストを画面表示用データ(Map)リストに変換
    public List<Map<String, Object>> convertToDisplayData(List<StudentEntity> students) {
        List<Map<String, Object>> displayList = new ArrayList<>();

        for (StudentEntity s : students) {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", s.getUserId());
            
            // ユーザー情報
            if (s.getUser() != null) {
                map.put("loginId", s.getUser().getLoginId());
                map.put("name", s.getUser().getName());
            } else {
                map.put("loginId", "");
                map.put("name", "");
            }

            // ステータス変換
            map.put("status", convertStatusIdToName(s.getStudentStatusId()));

            // 所属情報（Enrollments）の処理
            String grade = "-";
            String deptAndCourse = "-";
            String className = "-";
            
            try {
                if (s.getEnrollments() != null && !s.getEnrollments().isEmpty()) {
                    EnrollmentsEntity activeEnrollment = s.getEnrollments().stream()
                        .filter(e -> Boolean.TRUE.equals(e.getIsActive()))
                        .findFirst()
                        .orElse(s.getEnrollments().get(0));
    
                    if (activeEnrollment.getGrade() != null) {
                        grade = String.valueOf(activeEnrollment.getGrade());
                    }
                    
                    if (activeEnrollment.getDepartment() != null) {
                        className = activeEnrollment.getDepartment().getClassName();
                        
                        var major = activeEnrollment.getDepartment().getMajor();
                        if (major != null) {
                            String majorName = major.getMajorName();
                            String courseName = "";
                            if (major.getCourse() != null) {
                                courseName = major.getCourse().getCourseName();
                            }
                            deptAndCourse = majorName;
                            if (courseName != null && !courseName.isEmpty()) {
                                deptAndCourse += "・" + courseName;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 所属情報の取得に失敗した場合はデフォルト値を使用
                grade = "-";
                deptAndCourse = "-";
                className = "-";
            }
            
            map.put("grade", grade);
            map.put("department", deptAndCourse);
            map.put("classroom", className);

            displayList.add(map);
        }
        return displayList;
    }

    // ステータスIDをステータス名に変換
    private String convertStatusIdToName(Integer statusId) {
        if (statusId == null) return "-";
        switch (statusId) {
            case 1: return "在籍";
            case 2: return "退学";
            case 3: return "停学";
            case 4: return "卒業";
            case 5: return "除籍";
            case 6: return "休学";
            default: return "その他(" + statusId + ")";
        }
    }
}