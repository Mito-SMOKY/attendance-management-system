package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.SubjectFacultyRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository; // 名前が変わっている場合は UsersRepository に修正してください
import com.example.attendancemanagementsystem.user.admin.dto.SubjectMatrixRowDTO;

@Service
public class AdminSubjectService {

    @Autowired private SubjectRepository subjectRepository;
    // @Autowired private DepartmentRepository departmentRepository;
    // @Autowired private DepartmentSubjectRepository departmentSubjectRepository;
    
    @Autowired private UsersRepository usersRepository;             // ★追加: 教師データ取得用
    @Autowired private SubjectFacultyRepository subjectFacultyRepository; // ★追加: 中間テーブル操作用

    // --- 教科一覧取得（DBから教師情報・コマ数を結合して取得） ---
    public List<SubjectMatrixRowDTO> getSubjectInfoList() {
        List<SubjectEntity> subjects = subjectRepository.findAll();
        
        return subjects.stream().map(sub -> {
            SubjectMatrixRowDTO dto = new SubjectMatrixRowDTO();
            dto.setSubjectId(sub.getSubjectId());
            dto.setSubjectName(sub.getSubjectName());
            
            // コマ数設定 (RequiredCreditsを使用)
            dto.setCourseCount(sub.getRequiredCredits() != null ? sub.getRequiredCredits() : 1);
            
            // ★DBから実際の教師IDを取得
            Integer teacherId = subjectFacultyRepository.findTeacherIdBySubjectId(sub.getSubjectId());
            
            if (teacherId != null) {
                dto.setTeacherId(teacherId);
                // IDを元に教師名を取得
                usersRepository.findById(teacherId).ifPresent(user -> {
                    dto.setTeacherName(user.getName());
                });
            } else {
                dto.setTeacherId(null);
                dto.setTeacherName("未設定");
            }

            return dto;
        }).collect(Collectors.toList());
    }

    // --- 教師リスト取得（DBのusersテーブルから取得） ---
    public List<SubjectMatrixRowDTO> getTeacherList() {
        // ★ UserTypeID = 2 (管理者/教師) のユーザーをDBから取得
        // ※DBの定義に合わせて ID 2 が教師・管理者であることを想定しています
        List<UsersEntity> users = usersRepository.findByUserTypeId(2);
        
        return users.stream().map(user -> {
            SubjectMatrixRowDTO dto = new SubjectMatrixRowDTO();
            dto.setTeacherId(user.getUserId());
            dto.setTeacherName(user.getName());
            return dto;
        }).collect(Collectors.toList());
    }

    // --- 一括保存・削除処理 ---
    @Transactional
    public void saveSubjectList(
            List<Integer> subjectIds, 
            List<String> subjectNames, 
            List<Integer> teacherIds, 
            List<Integer> courseCounts) {
        
        if (subjectNames == null) return;

        // ==========================================
        // 1. 削除機能の実装
        // ==========================================
        // 画面から送信されたIDリストを作成（これに含まれないIDは削除対象）
        List<Integer> keptIds = new ArrayList<>();
        if (subjectIds != null) {
            for(Integer id : subjectIds) {
                if(id != null) keptIds.add(id);
            }
        }
        
        List<SubjectEntity> allSubjects = subjectRepository.findAll();
        for (SubjectEntity sub : allSubjects) {
            // 画面から消されたIDを見つけた場合
            if (!keptIds.contains(sub.getSubjectId())) {
                
                // ★重要: 外部キー制約エラーを防ぐため、先に中間テーブル(subjectfaculty)から削除
                subjectFacultyRepository.deleteBySubjectId(sub.getSubjectId());
                
                // その後、教科マスタ(subject)を削除
                subjectRepository.delete(sub); 
            }
        }

        // ==========================================
        // 2. 保存・更新処理
        // ==========================================
        for (int i = 0; i < subjectNames.size(); i++) {
            String name = subjectNames.get(i);
            
            // リストのインデックス範囲チェックを行いつつ値を取得
            Integer currentId = (subjectIds != null && subjectIds.size() > i) ? subjectIds.get(i) : null;
            Integer count = (courseCounts != null && courseCounts.size() > i) ? courseCounts.get(i) : 1; 
            Integer teacherId = (teacherIds != null && teacherIds.size() > i) ? teacherIds.get(i) : null;

            SubjectEntity entity;
            
            // IDがある場合は更新、なければ新規作成
            if (currentId != null) {
                entity = subjectRepository.findById(currentId).orElse(new SubjectEntity());
            } else {
                entity = new SubjectEntity();
            }

            // 教科情報のセット
            entity.setSubjectName(name);
            entity.setRequiredCredits(count); 
            
            // TotalCreditsも必須項目のためセット
            if (entity.getTotalCredits() == null) {
                entity.setTotalCredits(count); 
            }
            
            // ★まず教科を保存 (ここでSubjectIDが確定/更新される)
            SubjectEntity savedEntity = subjectRepository.save(entity);

            // ==========================================
            // 3. 教師情報の紐づけ保存
            // ==========================================
            // 一旦、この教科に関連する古い紐づけを削除（重複防止）
            subjectFacultyRepository.deleteBySubjectId(savedEntity.getSubjectId());

            // 教師が選択されている場合のみ新規登録
            if (teacherId != null) {
                subjectFacultyRepository.insertSubjectFaculty(savedEntity.getSubjectId(), teacherId);
            }
        }
    }
}