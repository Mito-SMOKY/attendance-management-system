package com.example.attendancemanagementsystem.user.admin.service;

import java.util.ArrayList;
import java.util.HashMap;
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

    //教科一覧の取得とフィルタリング
    public List<AdminSubjectListDto> getTeacherSubjects(String loginId, String search, List<Integer> grades, List<String> classes) {

        // 履修マスタ取得
        List<AdminSubjectListDto> allSubjects = getAllSubjectsRaw();

        return allSubjects.stream()

            // 指定された学年で絞り込み（未指定時はスキップ）
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

            // フィルタ結果をリストに変換して返却
            .collect(Collectors.toList());
    }

    
    //フィルタ選択肢の取得
    public Map<String, Object> getAvailableFilterOptions(String loginId, List<Integer> selectedGrades, List<String> selectedClasses) {

        // カリキュラムを取得）
        List<AdminSubjectListDto> allSubjects = getAllSubjectsRaw();

        // 学年の選択肢リストを作成
        Set<Integer> availableGrades = new TreeSet<>();
        
        // クラスが選択されている場合は、そのクラスが存在する学年のみ抽出、未選択なら全学年
        if (selectedClasses != null && !selectedClasses.isEmpty()) {
            allSubjects.stream()
                .filter(d -> selectedClasses.contains(d.getClassName()))
                .forEach(d -> availableGrades.add(d.getGrade()));
        } else {
            allSubjects.forEach(d -> availableGrades.add(d.getGrade()));
        }

        // クラスの選択肢リストを作成
        Set<String> availableClasses = new TreeSet<>();
        
        // 学年が選択されている場合は、その学年に存在するクラスのみ抽出、未選択なら全クラス
        if (selectedGrades != null && !selectedGrades.isEmpty()) {
            allSubjects.stream()
                .filter(d -> selectedGrades.contains(d.getGrade()))
                .forEach(d -> availableClasses.add(d.getClassName()));
        } else {
            allSubjects.forEach(d -> availableClasses.add(d.getClassName()));
        }

        // 結果をマップに格納して返却
        Map<String, Object> options = new HashMap<>();
        options.put("grades", new ArrayList<>(availableGrades));
        options.put("classes", new ArrayList<>(availableClasses));
        return options;
    }

    //全データを取得してDTOに変換
    private List<AdminSubjectListDto> getAllSubjectsRaw() {

        // 全教科・全クラスの組み合わせを取得
        List<Object[]> rawDataList = departmentSubjectRepository.findAllCurriculumRaw();

        // 取得した生データをDTOリストに変換
        List<AdminSubjectListDto> dtoList = new ArrayList<>();
        for (Object[] row : rawDataList) {
            Integer subjectId   = (Integer) row[0];
            String subjectName  = (String)  row[1];
            String courseName   = (String)  row[2];
            Integer grade       = (Integer) row[3];
            String className    = (String)  row[4];
            
            dtoList.add(new AdminSubjectListDto(subjectId, subjectName, courseName, grade, className));
        }
        return dtoList;
    }
}