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

import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.admin.dto.AdminSubjectListDto;

@Service
@Transactional(readOnly = true)
public class AdminSubjectListService {

    private final TimetableRepository timetableRepository;
    private final UsersRepository usersRepository;

    public AdminSubjectListService(TimetableRepository timetableRepository, UsersRepository usersRepository) {
        this.timetableRepository = timetableRepository;
        this.usersRepository = usersRepository;
    }

    //検索とフィルタリング
    public List<AdminSubjectListDto> getTeacherSubjects(String loginId, String search, List<Integer> grades, List<String> classes) {
        List<AdminSubjectListDto> allSubjects = getAllSubjectsRaw(loginId);

        return allSubjects.stream()

            // 学年フィルタ
            .filter(dto -> {
                if (grades == null || grades.isEmpty()) return true;
                return grades.contains(dto.getGrade());
            })

            // クラスフィルタ
            .filter(dto -> {
                if (classes == null || classes.isEmpty()) return true;
                return classes.contains(dto.getClassName());
            })

            // 教科名検索
            .filter(dto -> {
                if (search == null || search.isEmpty()) return true;
                String keyword = search.toLowerCase();
                return dto.getSubjectName() != null && dto.getSubjectName().toLowerCase().contains(keyword);
            })
            .collect(Collectors.toList());
    }

    //選択可能な学年とクラスのリストを返す
    public Map<String, Object> getAvailableFilterOptions(String loginId, List<Integer> selectedGrades, List<String> selectedClasses) {
        List<AdminSubjectListDto> allSubjects = getAllSubjectsRaw(loginId);

        // 学年リストの作成
        Set<Integer> availableGrades = new TreeSet<>();
        if (selectedClasses != null && !selectedClasses.isEmpty()) {

            // クラスが既に選ばれている場合 -> そのクラスを持つデータの学年だけを抽出
            allSubjects.stream()
                .filter(d -> selectedClasses.contains(d.getClassName()))
                .forEach(d -> availableGrades.add(d.getGrade()));
        } else {

            // クラス未選択 -> 全データの学年を表示
            allSubjects.forEach(d -> availableGrades.add(d.getGrade()));
        }

        // クラスリストの作成
        Set<String> availableClasses = new TreeSet<>();
        if (selectedGrades != null && !selectedGrades.isEmpty()) {

            // 学年が既に選ばれている場合 -> その学年を持つデータのクラスだけを抽出
            allSubjects.stream()
                .filter(d -> selectedGrades.contains(d.getGrade()))
                .forEach(d -> availableClasses.add(d.getClassName()));
        } else {

            // 学年未選択 -> 全データのクラスを表示
            allSubjects.forEach(d -> availableClasses.add(d.getClassName()));
        }

        Map<String, Object> options = new HashMap<>();
        options.put("grades", new ArrayList<>(availableGrades));
        options.put("classes", new ArrayList<>(availableClasses));
        return options;
    }

    // 全データ取得
    private List<AdminSubjectListDto> getAllSubjectsRaw(String loginId) {
        UsersEntity user = usersRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found: " + loginId));

        List<Object[]> rawDataList = timetableRepository.findTeacherSubjectsRaw(user.getUserId());

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