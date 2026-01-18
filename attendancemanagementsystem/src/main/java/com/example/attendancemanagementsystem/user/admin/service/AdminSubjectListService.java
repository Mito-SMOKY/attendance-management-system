package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap; 
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.user.admin.dto.AdminSubjectListDto;

@Service
@Transactional(readOnly = true)
public class AdminSubjectListService {

    private final DepartmentSubjectRepository departmentSubjectRepository;

    public AdminSubjectListService(DepartmentSubjectRepository departmentSubjectRepository) {
        this.departmentSubjectRepository = departmentSubjectRepository;
    }

    // 教科一覧の取得とフィルタリング
    public List<AdminSubjectListDto> getTeacherSubjects(String loginId, String search, List<Integer> grades, List<String> classes) {

        // 履修マスタ取得
        List<AdminSubjectListDto> allSubjects = getAllSubjectsRaw();

        return allSubjects.stream()

            // 指定された学年で絞り込み
            .filter(dto -> {
                if (grades == null || grades.isEmpty()) return true;
                return grades.contains(dto.getGrade());
            })

            // 指定されたクラスで絞り込み
            .filter(dto -> {
                if (classes == null || classes.isEmpty()) return true;
                return classes.contains(dto.getClassName());
            })

            // 教科名のキーワード検索
            .filter(dto -> {
                if (search == null || search.isEmpty()) return true;
                String keyword = search.toLowerCase();
                return dto.getSubjectName() != null && dto.getSubjectName().toLowerCase().contains(keyword);
            })
            .collect(Collectors.toList());
    }

    // フィルタ選択肢の取得
    public Map<String, Object> getAvailableFilterOptions(String loginId, List<Integer> selectedGrades, List<String> selectedClasses) {
        
        // 重複除去済みのデータを取得
        List<AdminSubjectListDto> allSubjects = getAllSubjectsRaw();

        // 学年の選択肢リストを作成
        Set<Integer> availableGrades = new TreeSet<>();
        if (selectedClasses != null && !selectedClasses.isEmpty()) {
            allSubjects.stream()
                .filter(d -> selectedClasses.contains(d.getClassName()))
                .forEach(d -> availableGrades.add(d.getGrade()));
        } else {
            allSubjects.forEach(d -> availableGrades.add(d.getGrade()));
        }

        // クラスの選択肢リストを作成
        Set<String> availableClasses = new TreeSet<>();
        if (selectedGrades != null && !selectedGrades.isEmpty()) {
            allSubjects.stream()
                .filter(d -> selectedGrades.contains(d.getGrade()))
                .forEach(d -> availableClasses.add(d.getClassName()));
        } else {
            allSubjects.forEach(d -> availableClasses.add(d.getClassName()));
        }

        Map<String, Object> options = new HashMap<>();
        options.put("grades", new ArrayList<>(availableGrades));
        options.put("classes", new ArrayList<>(availableClasses));
        return options;
    }

    // 全データを取得してDTOに変換
    private List<AdminSubjectListDto> getAllSubjectsRaw() {

        // 全教科・全クラスの組み合わせを取得
        List<Object[]> rawDataList = departmentSubjectRepository.findAllCurriculumRaw();

        //セット
        Map<String, AdminSubjectListDto> uniqueMap = new LinkedHashMap<>();
        for (Object[] row : rawDataList) {
            Integer departmentId = (Integer) row[0];
            Integer subjectId    = (Integer) row[1];
            String subjectName   = (String)  row[2];
            String courseName    = (String)  row[3];
            Integer grade        = (Integer) row[4];
            String className     = (String)  row[5];

            // 一意にするためのキーを作成（クラスIDと教科IDの組み合わせ）
            String uniqueKey = courseName + "_" + grade + "_" + className + "_" + subjectName;

            // まだマップに登録されていない場合のみ追加
            if (!uniqueMap.containsKey(uniqueKey)) {
                uniqueMap.put(uniqueKey, new AdminSubjectListDto(departmentId, subjectId, subjectName, courseName, grade, className));
            }
        }
        
        // マップの値（DTO）だけをリストにして返す
        return new ArrayList<>(uniqueMap.values());
    }
}