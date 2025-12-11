package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.List; // 必要
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.repository.DepartmentRepository;
import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.user.admin.dto.SubjectMatrixRowDTO;

@Service
public class AdminSubjectService {

    @Autowired private SubjectRepository subjectRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private DepartmentSubjectRepository departmentSubjectRepository;
    // @Autowired private UserRepository userRepository; // 教師データ取得用があれば追加

    // ... (既存のメソッド getAllClassNames, getSubjectMatrixData はそのまま) ...

    // --- ★追加: 教科一覧取得（教師情報付き） ---
    public List<SubjectMatrixRowDTO> getSubjectInfoList() {
        List<SubjectEntity> subjects = subjectRepository.findAll();
        
        return subjects.stream().map(sub -> {
            SubjectMatrixRowDTO dto = new SubjectMatrixRowDTO();
            dto.setSubjectId(sub.getSubjectId());
            dto.setSubjectName(sub.getSubjectName());
            
            // ★EntityにTeacherのリレーションやIDがある場合の想定
            // dto.setTeacherId(sub.getTeacherId());
            // if(sub.getTeacher() != null) {
            //    dto.setTeacherName(sub.getTeacher().getName());
            // }
            
            // ★仮実装（カラムがない場合のエラー回避用ダミー）
            // 実際はDBの値を入れてください
            dto.setTeacherId(1); 
            dto.setTeacherName("仮教師A"); 

            return dto;
        }).collect(Collectors.toList());
    }

    // --- ★追加: 教師リスト取得（プルダウン用） ---
    // ここでもSubjectMatrixRowDTOを「IDと名前のペア」として再利用します
    public List<SubjectMatrixRowDTO> getTeacherList() {
        // 本来は userRepository.findByRole(...) などで取得
        List<SubjectMatrixRowDTO> teachers = new ArrayList<>();
        
        // ダミーデータ生成
        SubjectMatrixRowDTO t1 = new SubjectMatrixRowDTO(); t1.setTeacherId(1); t1.setTeacherName("千太郎 先生");
        SubjectMatrixRowDTO t2 = new SubjectMatrixRowDTO(); t2.setTeacherId(2); t2.setTeacherName("川島 先生");
        SubjectMatrixRowDTO t3 = new SubjectMatrixRowDTO(); t3.setTeacherId(3); t3.setTeacherName("玉木 先生");
        
        teachers.add(t1); teachers.add(t2); teachers.add(t3);
        return teachers;
    }

    // --- ★追加: 一括保存処理 ---
    @Transactional
    public void saveSubjectList(List<Integer> subjectIds, List<String> subjectNames, List<Integer> teacherIds) {
        if (subjectNames == null) return;

        // 1. 今回送信されなかったIDを特定して削除（＝画面で削除された行）
        List<Integer> keptIds = new ArrayList<>();
        if (subjectIds != null) {
            for(Integer id : subjectIds) {
                if(id != null) keptIds.add(id);
            }
        }
        
        List<SubjectEntity> allSubjects = subjectRepository.findAll();
        for (SubjectEntity sub : allSubjects) {
            if (!keptIds.contains(sub.getSubjectId())) {
                // FK制約がある場合は deleteById などで関連テーブルも考慮が必要
                subjectRepository.delete(sub); 
            }
        }

        // 2. 更新 または 新規作成
        for (int i = 0; i < subjectNames.size(); i++) {
            String name = subjectNames.get(i);
            // Integer teacherId = (teacherIds != null && teacherIds.size() > i) ? teacherIds.get(i) : null;
            Integer currentId = (subjectIds != null && subjectIds.size() > i) ? subjectIds.get(i) : null;

            SubjectEntity entity;
            if (currentId != null) {
                entity = subjectRepository.findById(currentId).orElse(new SubjectEntity());
            } else {
                entity = new SubjectEntity();
            }

            entity.setSubjectName(name);
            // entity.setTeacherId(teacherId); // ★Entityのカラムに合わせてセットしてください
            
            subjectRepository.save(entity);
        }
    }
}