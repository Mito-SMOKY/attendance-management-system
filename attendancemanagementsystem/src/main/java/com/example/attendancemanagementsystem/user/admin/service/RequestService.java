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
import com.example.attendancemanagementsystem.common.entity.DepartmentEntity;
import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.RequestDetailEntity;
import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.StudentEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.AttendanceStatusRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
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
    
    @Autowired
    private DepartmentRepository departmentRepository;
    

    /**
     * ▼▼▼ 生徒選択画面用の検索メソッド ▼▼▼
     */
    public Page<Map<String, Object>> searchStudentsForSelection(String mode, String keyword, Pageable pageable) {
        List<StudentEntity> allStudents = studentRepository.findAll();
        
        List<StudentEntity> filteredStudents = allStudents;
        if (keyword != null && !keyword.isEmpty()) {
            filteredStudents = allStudents.stream()
                .filter(s -> s.getUser().getName().contains(keyword))
                .collect(Collectors.toList());
        }

        if ("delete".equals(mode)) {
            filteredStudents = filteredStudents.stream()
                .filter(s -> {
                    Integer sid = s.getStudentStatusId();
                    return sid != null && (sid == 2 || sid == 4 || sid == 5);
                })
                .collect(Collectors.toList());
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredStudents.size());
        List<StudentEntity> pagedList = new ArrayList<>();
        if (start <= end) {
            pagedList = filteredStudents.subList(start, end);
        }

        List<Map<String, Object>> content = pagedList.stream().map(student -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put("userId", student.getUserId());
            map.put("name", student.getUser().getName());
            map.put("loginId", student.getUser().getLoginId());
            
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
     * 承認処理
     */
    public void approveRequest(Integer requestId, Integer approverId) {
        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("指定された申請が見つかりません (ID: " + requestId + ")"));

        if (request.getStatus() != 1) {
            throw new IllegalArgumentException("この申請は既に処理されています。");
        }

        // 申請種別に応じた処理を実行
        Integer typeId = request.getRequestTypeId();
        if (typeId != null) {
            switch (typeId) {
                case 1: // 公欠申請
                    processPublicAbsence(request);
                    break;
                case 3: // ステータス変更申請
                    processStatusChange(request);
                    break;
                case 4: // 学科コース変更申請
                    processDepartmentChange(request);
                    break;
                default:
                    // その他の申請（アカウント削除など）は現状ステータス更新のみ
                    break;
            }
        }

        request.setStatus(2);
        request.setApproverId(approverId);
        requestRepository.save(request);
    }

    /**
     * 公欠申請の処理
     */
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
             throw new IllegalArgumentException("申請の詳細情報（日付・時限）がありません。");
        }

        for (RequestDetailEntity detail : details) {
            SessionEntity session = sessionRepository.findExistingSessionForApproval(
                detail.getTargetDate(),
                detail.getSlotId(),
                deptId,
                grade
            ).orElseThrow(() -> new IllegalArgumentException(
                "授業がまだ実施されていません (日付: " + detail.getTargetDate() + ", " + detail.getSlotId() + "限)。実施後に承認してください。"
            ));

            AttendanceEntity attendance = attendanceRepository.findBySessionIdAndUserId(
                session.getSessionId(), 
                studentId
            ).orElse(null);

            if (attendance != null) {
                attendance.setStatusId(publicAbsenceStatus);
                attendanceRepository.save(attendance);
            } else {
                throw new IllegalArgumentException(
                    "対象授業の出席データが見つかりません (日付: " + detail.getTargetDate() + ")。出席簿が作成されているか確認してください。"
                );
            }
        }
    }

    /**
     * 学科・コース変更申請の処理 (Type 4)
     */
    private void processDepartmentChange(RequestEntity request) {
        Integer targetDeptId = request.getTargetDepartmentId();
        if (targetDeptId == null) {
            throw new IllegalArgumentException("変更後の学科IDが指定されていません。");
        }

        // 変更後の学科存在チェック
        DepartmentEntity newDept = departmentRepository.findById(targetDeptId)
            .orElseThrow(() -> new IllegalArgumentException("指定された学科ID (" + targetDeptId + ") が見つかりません。"));

        Integer studentId = request.getRequesterUserId();
        StudentEntity student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("生徒情報が見つかりません (ID: " + studentId + ")"));

        // 現在の所属情報を取得して更新
        List<EnrollmentsEntity> enrollments = enrollmentsRepository.findByStudent(student);
        if (enrollments.isEmpty()) {
            throw new IllegalArgumentException("生徒の在籍情報(Enrollments)が存在しないため更新できません。");
        }

        // 最新の所属（または全て）を更新する運用と仮定
        // 通常は IsActive=true のレコードが1つあるはず
        EnrollmentsEntity activeEnrollment = enrollments.get(0);
        
        // 学科を更新
        activeEnrollment.setDepartment(newDept);
        enrollmentsRepository.save(activeEnrollment);
    }

    /**
     * ステータス変更申請の処理 (Type 3)
     */
    private void processStatusChange(RequestEntity request) {
        Integer targetStatusId = request.getTargetStatusId();
        if (targetStatusId == null) {
            throw new IllegalArgumentException("変更後のステータスIDが指定されていません。");
        }

        // マスタ存在チェック（任意）
        // studentStatusRepository.findById(targetStatusId)...

        Integer studentId = request.getRequesterUserId();
        StudentEntity student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("生徒情報が見つかりません (ID: " + studentId + ")"));

        // 生徒のステータスを更新
        student.setStudentStatusId(targetStatusId);
        studentRepository.save(student);
    }
}