package com.example.attendancemanagementsystem.user.admin.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
import com.example.attendancemanagementsystem.common.service.SearchService;

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

    // ★追加: SearchServiceを利用
    @Autowired
    private SearchService searchService;
    

    /**
     * ▼▼▼ 生徒選択画面用の検索メソッド ▼▼▼
     */
    public Page<Map<String, Object>> searchStudentsForSelection(String mode, String keyword, Pageable pageable) {
        
        // 1. 基本条件: 削除フラグがFalseのユーザー
        Specification<StudentEntity> spec = (root, query, cb) -> 
            cb.isFalse(root.get("user").get("deleteFlag"));

        // 2. キーワード検索 (SearchService利用)
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 検索対象カラム: 学籍番号(loginId), 名前(name), ステータス名(studentStatusName)
            List<String> searchColumns = Arrays.asList(
                "user.loginId", 
                "user.name", 
                "studentStatus.studentStatusName" // StudentEntityに追加したリレーションを使用
            );
            
            Specification<StudentEntity> keywordSpec = searchService.createKeywordSpec(keyword, searchColumns);
            spec = spec.and(keywordSpec);
        }

        // 3. モード別フィルタ (削除申請モードなどの場合)
        if ("delete".equals(mode)) {
            // 退学(2), 卒業(4), 除籍(5) の生徒のみ対象とする場合の例
            Specification<StudentEntity> modeSpec = (root, query, cb) -> 
                root.get("studentStatusId").in(Arrays.asList(2, 4, 5));
            spec = spec.and(modeSpec);
        }

        // 4. 検索実行 (Specification + Pageable)
        Page<StudentEntity> studentPage = studentRepository.findAll(spec, pageable);

        // 5. 結果をMapに変換 (画面表示用)
        List<Map<String, Object>> content = studentPage.getContent().stream().map(student -> {
            Map<String, Object> map = new HashMap<>();
            
            map.put("userId", student.getUserId());
            // UserEntity等はLazyロードされる可能性があるため、必要に応じてフェッチ結合を検討するか、
            // ここでのN+1を許容します（ページサイズが小さければ大きな問題にはなりにくい）
            if (student.getUser() != null) {
                map.put("name", student.getUser().getName());
                map.put("loginId", student.getUser().getLoginId());
            } else {
                map.put("name", "-");
                map.put("loginId", "-");
            }
            
            Integer statusId = student.getStudentStatusId();
            String statusName = "-";
            
            // StudentEntityに追加したリレーションを使って名前を取得することも可能ですが、
            // 既存ロジックに合わせてID分岐で表示するか、Entityから取得するか選択できます。
            // ここではEntityから取得する例：
            if (student.getStudentStatus() != null) {
                statusName = student.getStudentStatus().getStudentStatusName();
            } else if (statusId != null) {
                 // フォールバック（既存ロジック）
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

            // 在籍情報の取得（ここは既存通りRepository経由）
            List<EnrollmentsEntity> enrollments = enrollmentsRepository.findByStudent(student);
            if (!enrollments.isEmpty()) {
                EnrollmentsEntity e = enrollments.get(0);
                if (e.getDepartment() != null && e.getDepartment().getMajor() != null) {
                    map.put("department", e.getDepartment().getMajor().getMajorName());
                    map.put("classroom", e.getDepartment().getClassName());
                } else {
                     map.put("department", "-");
                     map.put("classroom", "-");
                }
                map.put("grade", e.getGrade());
            } else {
                map.put("department", "-");
                map.put("grade", "-");
                map.put("classroom", "-");
            }

            boolean isPending = requestRepository.existsByRequesterUserIdAndStatus(student.getUserId(), 1);
            map.put("isPending", isPending);
            
            return map;
        }).collect(Collectors.toList());

        return new PageImpl<>(content, pageable, studentPage.getTotalElements());
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
                    break;
            }
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

    private void processDepartmentChange(RequestEntity request) {
        Integer targetDeptId = request.getTargetDepartmentId();
        if (targetDeptId == null) {
            throw new IllegalArgumentException("変更後の学科IDが指定されていません。");
        }

        DepartmentEntity newDept = departmentRepository.findById(targetDeptId)
            .orElseThrow(() -> new IllegalArgumentException("指定された学科ID (" + targetDeptId + ") が見つかりません。"));

        Integer studentId = request.getRequesterUserId();
        StudentEntity student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("生徒情報が見つかりません (ID: " + studentId + ")"));

        List<EnrollmentsEntity> enrollments = enrollmentsRepository.findByStudent(student);
        if (enrollments.isEmpty()) {
            throw new IllegalArgumentException("生徒の在籍情報(Enrollments)が存在しないため更新できません。");
        }

        EnrollmentsEntity activeEnrollment = enrollments.get(0);
        activeEnrollment.setDepartment(newDept);
        enrollmentsRepository.save(activeEnrollment);
    }

    private void processStatusChange(RequestEntity request) {
        Integer targetStatusId = request.getTargetStatusId();
        if (targetStatusId == null) {
            throw new IllegalArgumentException("変更後のステータスIDが指定されていません。");
        }

        Integer studentId = request.getRequesterUserId();
        StudentEntity student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("生徒情報が見つかりません (ID: " + studentId + ")"));

        student.setStudentStatusId(targetStatusId);
        studentRepository.save(student);
    }
}