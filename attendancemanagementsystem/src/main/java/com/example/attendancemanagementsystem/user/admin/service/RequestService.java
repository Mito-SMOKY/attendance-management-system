package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.AttendanceEntity;
import com.example.attendancemanagementsystem.common.entity.AttendanceStatusEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.RequestDetailEntity;
import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.StudentRepository;

@Service("adminRequestService")
@Transactional
public class RequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EnrollmentsRepository enrollmentsRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceStatusRepository attendanceStatusRepository;

    /**
     * ▼▼▼ 生徒選択画面用の検索メソッド ▼▼▼
     */
    public Page<Map<String, Object>> searchStudentsForSelection(String mode, String keyword, Pageable pageable) {
        List<StudentEntity> allStudents = studentRepository.findAll();
        
        // 1. キーワード検索
        List<StudentEntity> filteredStudents = allStudents;
        if (keyword != null && !keyword.isEmpty()) {
            filteredStudents = allStudents.stream()
                .filter(s -> s.getUser().getName().contains(keyword))
                .collect(Collectors.toList());
        }

        // 2. モードによるフィルタリング (ここを追加)
        if ("delete".equals(mode)) {
            // 削除モードの場合: 退学(2), 卒業(4), 除籍(5) の生徒のみ表示
            filteredStudents = filteredStudents.stream()
                .filter(s -> {
                    Integer sid = s.getStudentStatusId();
                    return sid != null && (sid == 2 || sid == 4 || sid == 5);
                })
                .collect(Collectors.toList());
        }

        // 3. ページネーション
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredStudents.size());
        List<StudentEntity> pagedList = new ArrayList<>();
        if (start <= end) {
            pagedList = filteredStudents.subList(start, end);
        }

        // 4. 表示用データの作成
        List<Map<String, Object>> content = pagedList.stream().map(student -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put("userId", student.getUserId());
            map.put("name", student.getUser().getName());
            map.put("loginId", student.getUser().getLoginId());
            
            // ステータス名のマッピング (いただいたデータに基づいて修正)
            Integer statusId = student.getStudentStatusId();
            String statusName = "-";
            if (statusId != null) {
                switch (statusId) {
                    case 1: statusName = "在籍"; break;
                    case 2: statusName = "退学"; break;
                    case 3: statusName = "停学"; break;
                    case 4: statusName = "卒業"; break;
                    case 5: statusName = "除籍"; break;
                    case 6: statusName = "休学"; break;
                    default: statusName = "その他(" + statusId + ")"; break;
                }
            }
            map.put("status", statusName);

            List<EnrollmentsEntity> enrollments = enrollmentsRepository.findByStudent(student);
            if (!enrollments.isEmpty()) {
                EnrollmentsEntity e = enrollments.get(0);
                map.put("department", e.getDepartment().getMajor().getMajorName());
                map.put("grade", e.getGrade());
                map.put("classroom", e.getDepartment().getClassName());
            } else {
                map.put("department", "-");
                map.put("grade", "-");
                map.put("classroom", "-");
            }

            boolean isPending = requestRepository.existsByRequesterUserIdAndStatus(student.getUserId(), 1);
            map.put("isPending", isPending);
            
            return map;
        }).collect(Collectors.toList());

        return new PageImpl<>(content, pageable, filteredStudents.size());
    }

    /**
     * 承認処理 (公欠のみ対応)
     */
    public void approveRequest(Integer requestId, Integer approverId) {
        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("指定された申請が見つかりません (ID: " + requestId + ")"));

        if (request.getStatus() != 1) {
            throw new IllegalArgumentException("この申請は既に処理されています。");
        }

        // 公欠(Type 1)のみ処理
        if (request.getRequestTypeId() == 1) {
            processPublicAbsence(request);
        }

        request.setStatus(2);
        request.setApproverId(approverId);
        requestRepository.save(request);
    }

    private void processPublicAbsence(RequestEntity request) {
        Integer studentId = request.getRequesterUserId();
        StudentEntity student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("生徒情報が見つかりません (ID: " + studentId + ")"));

        List<EnrollmentsEntity> enrollments = enrollmentsRepository.findByStudent(student);
        if (enrollments.isEmpty()) {
            throw new IllegalArgumentException("生徒の在籍情報が見つかりません。");
        }
        
        EnrollmentsEntity currentEnrollment = enrollments.get(0); 
        Integer deptId = currentEnrollment.getDepartment().getDepartmentId();
        Integer grade = currentEnrollment.getGrade();

        AttendanceStatusEntity publicAbsenceStatus = attendanceStatusRepository.findById(4)
            .orElseThrow(() -> new IllegalArgumentException("ステータスID:4 (公欠) がマスタに見つかりません。"));

        List<RequestDetailEntity> details = request.getRequestDetails();
        if (details == null || details.isEmpty()) {
            throw new IllegalStateException("申請の詳細情報（日付・時限）がありません。");
        }

        for (RequestDetailEntity detail : details) {
            SessionEntity session = sessionRepository.findExistingSessionForApproval(
                detail.getTargetDate(),
                detail.getSlotId(),
                deptId,
                grade
            ).orElseThrow(() -> new IllegalArgumentException(
                "承認エラー: 指定された日時（" + detail.getTargetDate() + " / " + detail.getSlotId() + "限）の授業データがまだ確定していない、または存在しないため承認できません。"
            ));

            AttendanceEntity attendance = attendanceRepository.findBySessionIdAndUserId(
                session.getSessionId(), 
                studentId
            ).orElseThrow(() -> new IllegalArgumentException(
                "承認エラー: 授業データは確定されていますが、対象学生の出席記録が存在しません。"
            ));

            attendance.setStatusId(publicAbsenceStatus);
            attendanceRepository.save(attendance);
        }
    }
}