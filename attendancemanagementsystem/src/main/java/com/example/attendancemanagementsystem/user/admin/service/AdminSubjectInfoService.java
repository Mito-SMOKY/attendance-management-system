package com.example.attendancemanagementsystem.user.admin.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays; // 追加
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.jpa.domain.Specification; // 追加
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.DepartmentSubject;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.service.SearchService; // 追加
import com.example.attendancemanagementsystem.user.admin.dto.AdminSubjectInfoDto;

@Service
@Transactional(readOnly = true)
public class AdminSubjectInfoService {

    private final DepartmentSubjectRepository departmentSubjectRepository;
    private final TimetableRepository timetableRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentsRepository enrollmentsRepository;
    private final SearchService searchService; // ★追加

    public AdminSubjectInfoService(
            DepartmentSubjectRepository departmentSubjectRepository,
            TimetableRepository timetableRepository,
            SubjectRepository subjectRepository,
            EnrollmentsRepository enrollmentsRepository,
            SearchService searchService) { // ★追加
        this.departmentSubjectRepository = departmentSubjectRepository;
        this.timetableRepository = timetableRepository;
        this.subjectRepository = subjectRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.searchService = searchService;
    }

    // --- (getSubjectInfo メソッドなどは変更なし。ただしDTOのプルダウン用リスト作成処理は削除してもOKです) ---
    public AdminSubjectInfoDto getSubjectInfo(Integer departmentId, Integer subjectId, Integer grade) {
        AdminSubjectInfoDto dto = new AdminSubjectInfoDto();
        dto.setDepartmentId(departmentId);
        dto.setSubjectId(subjectId);
        dto.setGrade(grade);
        
        dto.setFiscalYear(calculateFiscalYear());
        dto.setDisplaySubjectId(String.format("%04d-%02d", subjectId, departmentId));

        // 1. コース・クラス名
        List<Object[]> deptInfoList = departmentSubjectRepository.findCourseAndClass(departmentId);
        if (deptInfoList != null && !deptInfoList.isEmpty()) {
            Object[] row = deptInfoList.get(0);
            String courseName = (String) row[0];
            String className = (String) row[1];
            dto.setCourseAndGrade(String.format("%s %d年 %s", courseName, grade, className));
        } else {
            dto.setCourseAndGrade("-");
        }

        // 2. 教科情報
        SubjectEntity subject = subjectRepository.findById(subjectId).orElse(null);
        if (subject != null) {
            dto.setSubjectName(subject.getSubjectName());
        }
        
        // ※以前追加した「全件取得ループ」は重いので削除し、検索機能に委ねます

        // 3. 担当教員
        List<String> teacherNames = timetableRepository.findTeacherNameBySubjectAndClass(subjectId, departmentId);
        dto.setTeacherName(teacherNames != null && !teacherNames.isEmpty() ? teacherNames.get(0) : "未定");

        // 4. スケジュール
        List<Object[]> scheduleData = timetableRepository.findScheduleAndRoom(subjectId, departmentId);
        Set<Integer> dayNumSet = new TreeSet<>();
        Set<String> roomSet = new HashSet<>();
        if (scheduleData != null) {
            dto.setTotalClasses(scheduleData.size() + "コマ");
            for (Object[] row : scheduleData) {
                Object dateObj = row[0];
                String room = (String) row[1];
                if (dateObj != null) {
                    LocalDate date = null;
                    if (dateObj instanceof java.sql.Date) {
                        date = ((java.sql.Date) dateObj).toLocalDate();
                    } else if (dateObj instanceof java.time.LocalDate) {
                        date = (java.time.LocalDate) dateObj;
                    }
                    if (date != null) dayNumSet.add(date.getDayOfWeek().getValue());
                }
                if (room != null && !room.isEmpty()) roomSet.add(room);
            }
        }
        List<String> dayStrings = new ArrayList<>();
        for (Integer dayNum : dayNumSet) {
            dayStrings.add(convertDayToKanji(dayNum));
        }
        dto.setSchedule(dayStrings.isEmpty() ? "未定" : String.join("、", dayStrings));
        dto.setClassroom(roomSet.isEmpty() ? "未定" : String.join(", ", roomSet));

        // 5. 学生リスト
        List<Object[]> studentData = enrollmentsRepository.findStudentIdAndNamesByClass(departmentId, grade);
        List<AdminSubjectInfoDto.StudentSimpleInfo> studentList = new ArrayList<>();
        
        if (studentData != null) {
            for (Object[] row : studentData) {
                // row[0]=userId(Integer), row[1]=name(String)
                Integer sId = (Integer) row[0];
                String sName = (String) row[1];
                // DTOのコンストラクタに合わせてIDと名前をセット
                studentList.add(new AdminSubjectInfoDto.StudentSimpleInfo(sId, sName));
            }
        }
        dto.setStudents(studentList);

        return dto;
    }

    // --- ★追加: オートコンプリート用検索メソッド ---
    public List<AdminSubjectInfoDto.SubjectOption> searchSubjects(String query) {
        // SearchServiceを使って検索条件を作成 (対象カラム: subjectName)
        Specification<SubjectEntity> spec = searchService.createKeywordSpec(query, Arrays.asList("subjectName"));
        
        // 検索実行
        List<SubjectEntity> subjects = subjectRepository.findAll(spec);
        
        // 検索結果をDTOに変換（遷移先の解決を含む）
        List<AdminSubjectInfoDto.SubjectOption> results = new ArrayList<>();
        List<DepartmentSubject> allRelations = departmentSubjectRepository.findAll(); // 紐付け全件(キャッシュ推奨だが今回は簡易実装)

        for (SubjectEntity s : subjects) {
            // この教科が使われている場所を探す（最初に見つかったもの）
            DepartmentSubject match = allRelations.stream()
                .filter(r -> r.getId().getSubjectId().equals(s.getSubjectId()))
                .findFirst().orElse(null);

            if (match != null) {
                results.add(new AdminSubjectInfoDto.SubjectOption(
                    s.getSubjectId(),
                    s.getSubjectName(),
                    match.getId().getDepartmentId(),
                    match.getGrade()
                ));
            }
        }
        return results;
    }

    private String convertDayToKanji(Integer dayNum) {
        switch (dayNum) {
            case 1: return "月曜";
            case 2: return "火曜";
            case 3: return "水曜";
            case 4: return "木曜";
            case 5: return "金曜";
            case 6: return "土曜";
            case 7: return "日曜";
            default: return "";
        }
    }

    private String calculateFiscalYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        if (now.getMonthValue() < 4) year -= 1;
        return year + "年度";
    }
}