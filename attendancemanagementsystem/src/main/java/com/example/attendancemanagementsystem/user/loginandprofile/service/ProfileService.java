package com.example.attendancemanagementsystem.user.loginandprofile.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.loginandprofile.dto.ProfileSubjectDto;
import com.example.attendancemanagementsystem.user.loginandprofile.dto.UserProfileDto;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final UsersRepository usersRepository;
    private final EnrollmentsRepository enrollmentsRepository;
    private final TimetableRepository timetableRepository;
    private final SubjectRepository subjectRepository;

    public ProfileService(UsersRepository usersRepository,
                        EnrollmentsRepository enrollmentsRepository,
                        TimetableRepository timetableRepository,
                        SubjectRepository subjectRepository) {
        this.usersRepository = usersRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.timetableRepository = timetableRepository;
        this.subjectRepository = subjectRepository;
    }

    // 1. ユーザー情報の取得
    public UserProfileDto getUserProfile(String loginId) {
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String roleName = "ゲスト";
        if (user.getUserTypeId() == 1) roleName = "学生";
        else if (user.getUserTypeId() == 2) roleName = "管理者";

        return new UserProfileDto(user.getName(), roleName, "Asia/Tokyo");
    }

    // 2. 教科リストの取得
    public List<ProfileSubjectDto> getUserSubjects(String loginId) {
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ProfileSubjectDto> subjectList = new ArrayList<>();

        if (user.getUserTypeId() == 1) {
            // --- 学生の場合: 所属学科の時間割にある教科を取得 ---
            EnrollmentsEntity enrollment = enrollmentsRepository.findByUserAndIsActiveTrue(user)
                    .orElse(null);
            
            if (enrollment != null) {
                Integer deptId = enrollment.getDepartment().getDepartmentId();
                // 重複排除して教科を取得
                List<TimetableEntity> timetables = timetableRepository.findDistinctSubjectsByDepartment(deptId);
                
                for (TimetableEntity tt : timetables) {
                    Integer sId = tt.getSubjectId();
                    String sName = subjectRepository.findById(sId)
                            .map(SubjectEntity::getSubjectName)
                            .orElse("不明な教科");
                    
                    // 重複チェックしてリストに追加
                    boolean exists = subjectList.stream().anyMatch(d -> d.getSubjectID().equals(sId));
                    if (!exists) {
                        subjectList.add(new ProfileSubjectDto(sId, sName));
                    }
                }
            }

        } else if (user.getUserTypeId() == 2) {
            // --- 管理者の場合: 担当教科テーブルから取得 ---
            List<SubjectEntity> subjects = subjectRepository.findSubjectsByTeacherId(user.getUserId());
            
            subjectList = subjects.stream()
                    .map(s -> new ProfileSubjectDto(s.getSubjectId(), s.getSubjectName()))
                    .collect(Collectors.toList());
        }

        return subjectList;
    }
}