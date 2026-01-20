package com.example.attendancemanagementsystem.user.loginandprofile.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.EnrollmentsEntity;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.entity.TimetableEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectFacultyRepository;
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
    private final SubjectFacultyRepository subjectFacultyRepository;

    // 各リポジトリの依存関係をコンストラクタで初期化
    public ProfileService(UsersRepository usersRepository,
                        EnrollmentsRepository enrollmentsRepository,
                        TimetableRepository timetableRepository,
                        SubjectRepository subjectRepository,
                        SubjectFacultyRepository subjectFacultyRepository) {
        this.usersRepository = usersRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.timetableRepository = timetableRepository;
        this.subjectRepository = subjectRepository;
        this.subjectFacultyRepository = subjectFacultyRepository;
    }

    public UserProfileDto getUserProfile(String loginId) {

        // ログインIDからユーザ情報を取得
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // ロール名決定
        String displayRole = (user.getUserTypeId() == 1) ? "学生" : "管理者";
        
        // プロフィール情報DTOを生成して返却
        return new UserProfileDto(user.getUserId(), user.getName(), displayRole, user.getEmail());        
    }

    @Transactional
    public void updateUserName(String loginId, String newName) {

        // 更新対象のユーザを取得
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 名前を更新して保存
        user.setName(newName);
        usersRepository.save(user);
    }

    public List<ProfileSubjectDto> getUserSubjects(String loginId) {

        // ログインIDからユーザを取得
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 返却用の教科リストを初期化
        List<ProfileSubjectDto> subjectList = new ArrayList<>();

        //生徒用
        if (user.getUserTypeId() == 1) {
            
            // 在籍情報を取得
            EnrollmentsEntity enrollment = enrollmentsRepository.findByUserAndIsActiveTrue(user)
                    .orElse(null);

            if (enrollment != null) {

                // 学科IDに紐づく時間割から教科リストを取得
                Integer deptId = enrollment.getDepartment().getDepartmentId();
                List<TimetableEntity> timetables = timetableRepository.findDistinctSubjectsByDepartment(deptId);
                
                // 取得した教科をループ処理
                for (TimetableEntity tt : timetables) {
                    Integer sId = tt.getSubjectId();
                    
                    // 教科IDから教科名を取得
                    String sName = subjectRepository.findById(sId)
                            .map(SubjectEntity::getSubjectName)
                            .orElse("不明な教科");
                    
                    // リストに未登録の場合のみ追加
                    if (subjectList.stream().noneMatch(d -> d.getSubjectId().equals(sId))) {
                        subjectList.add(new ProfileSubjectDto(sId, sName));
                    }
                }
            }
        
        //  管理者用
        } else if (user.getUserTypeId() == 2) {
            
            // 教科担当テーブルから担当教科・クラス・学年情報を取得
            List<Object[]> rawData = subjectFacultyRepository.findSubjectDetailsByTeacherId(user.getUserId());
            
            // 表示名の重複を防ぐためのセットを初期化
            Set<String> processedNames = new HashSet<>();

            // 取得したデータを1件ずつ処理
            for (Object[] row : rawData) {

                // 各カラム（ID, 名前, 学科, 学年）を取得
                Integer sId = (Integer) row[0];
                String sName = (String) row[1];
                Integer deptId = (Integer) row[2];
                Integer grade = (Integer) row[3];
                
                // クラス名がnullの場合はハイフンに置換
                String className = (row[4] != null) ? (String) row[4] : "-"; 
                
                // データ整形
                String displayName = String.format("%s (%d年 %s)", sName, grade, className);

                // 重複排除
                if (processedNames.contains(displayName)) {
                    continue; 
                }

                // 名称をセットに記録し、結果リストに追加
                processedNames.add(displayName);
                subjectList.add(new ProfileSubjectDto(sId, displayName, deptId, grade));
            }
        }
        return subjectList;
    }
}