package com.example.attendancemanagementsystem.attendance.display.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.display.dto.SubjectListDto;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.AttendanceRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
@Transactional(readOnly = true)
//科目別出席情報一覧を提供するサービスクラス
public class SubjectListService {

    private final UsersRepository usersRepository;
    private final EnrollmentsRepository enrollmentsRepository;
    private final TimetableRepository timetableRepository;
    private final AttendanceRepository attendanceRepository;
    private final SubjectRepository subjectRepository;
    private final SessionRepository sessionRepository; 

    public SubjectListService(
            UsersRepository usersRepository,
            EnrollmentsRepository enrollmentsRepository,
            TimetableRepository timetableRepository,
            AttendanceRepository attendanceRepository,
            SubjectRepository subjectRepository,
            SessionRepository sessionRepository) {
        this.usersRepository = usersRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.timetableRepository = timetableRepository;
        this.attendanceRepository = attendanceRepository;
        this.subjectRepository = subjectRepository;
        this.sessionRepository = sessionRepository;
    }

    // 科目別出席情報一覧を取得
    public List<SubjectListDto> getSubjectList(String loginId) {
        List<SubjectListDto> dtoList = new ArrayList<>();

        // ユーザー特定
        UsersEntity user = usersRepository.findByLoginId(loginId).orElseThrow();

        // 在籍情報取得
        var enrollmentOpt = enrollmentsRepository.findByUserAndIsActiveTrue(user);
        if (enrollmentOpt.isEmpty()) {
            return dtoList;
        }
        Integer deptId = enrollmentOpt.get().getDepartment().getDepartmentId();
        String courseName = enrollmentOpt.get().getDepartment().getMajor().getMajorName();

        // 当該学科の時間割から科目リストを取得（重複排除）
        List<TimetableEntity> allTimetables = timetableRepository.findDistinctSubjectsByDepartment(deptId);
        
        // 科目ID重複排除
        Map<Integer, TimetableEntity> uniqueSubjectsMap = allTimetables.stream()
                .collect(Collectors.toMap(
                        TimetableEntity::getSubjectId, 
                        t -> t, 
                        (existing, replacement) -> existing
                ));

        // 各科目について出席情報を計算
        for (TimetableEntity tt : uniqueSubjectsMap.values()) {
            Integer subjectId = tt.getSubjectId();

            // 科目名取得
            String subjectName = subjectRepository.findById(subjectId)
                    .map(s -> s.getSubjectName()).orElse("ID:" + subjectId);

            // 担当教員名取得
            String teacherName = usersRepository.findById(tt.getUserId())
                    .map(u -> u.getName()).orElse("未定");

            SubjectListDto dto = new SubjectListDto(subjectId, courseName, subjectName, teacherName);
            
            // -------------------------------------------------
            // ★計算ロジック修正 (公欠を出席扱いに変更)
            // -------------------------------------------------

            // 1. 全授業数 (Sessionから取得: 実施済みのみ)
            int rawTotal = sessionRepository.countImplementedSessions(deptId, subjectId);

            // 2. 出席停止の数 (分母から引く)
            // StatusID 6: 出席停止
            int suspensions = attendanceRepository.countByStatusTotal(user.getUserId(), subjectId, 6);

            // ※公欠(StatusID: 4) は分母から引かない
            
            // 3. 有効な授業数(分母) = 全実施授業数 - 出席停止
            int effectiveTotal = Math.max(0, rawTotal - suspensions);
            dto.setTotalClasses(effectiveTotal);

            // 4. 出席数(分子) = 出席(StatusID: 1) + 公欠(StatusID: 4)
            int attendedCount = attendanceRepository.countByStatusTotal(user.getUserId(), subjectId, 1);
            int publicAbsenceCount = attendanceRepository.countByStatusTotal(user.getUserId(), subjectId, 4);
            
            dto.setAttendedClasses(attendedCount + publicAbsenceCount);

            // -------------------------------------------------

            dto.calculateRate();
            dtoList.add(dto);
        }

        return dtoList;
    }
}