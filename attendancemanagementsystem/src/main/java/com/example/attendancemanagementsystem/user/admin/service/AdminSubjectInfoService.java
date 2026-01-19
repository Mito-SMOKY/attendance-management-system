package com.example.attendancemanagementsystem.user.admin.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.DepartmentSubject;
import com.example.attendancemanagementsystem.common.entity.SubjectEntity;
import com.example.attendancemanagementsystem.common.repository.DepartmentSubjectRepository;
import com.example.attendancemanagementsystem.common.repository.EnrollmentsRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.service.SearchService; 
import com.example.attendancemanagementsystem.user.admin.dto.AdminSubjectInfoDto;

@Service
@Transactional(readOnly = true)
public class AdminSubjectInfoService {

    private final DepartmentSubjectRepository departmentSubjectRepository;
    private final TimetableRepository timetableRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentsRepository enrollmentsRepository;
    private final SearchService searchService; 

    // コンストラクタインジェクション
    public AdminSubjectInfoService(
            DepartmentSubjectRepository departmentSubjectRepository,
            TimetableRepository timetableRepository,
            SubjectRepository subjectRepository,
            EnrollmentsRepository enrollmentsRepository,
            SearchService searchService) {
        this.departmentSubjectRepository = departmentSubjectRepository;
        this.timetableRepository = timetableRepository;
        this.subjectRepository = subjectRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.searchService = searchService;
    }

    //教科詳細画面に必要な情報をまとめて取得するメソッド
    public AdminSubjectInfoDto getSubjectInfo(Integer departmentId, Integer subjectId, Integer grade) {

        // 基本情報をセット
        AdminSubjectInfoDto dto = new AdminSubjectInfoDto();
        dto.setDepartmentId(departmentId);
        dto.setSubjectId(subjectId);
        dto.setGrade(grade);
        dto.setFiscalYear(calculateFiscalYear());
        dto.setDisplaySubjectId(String.format("%04d-%02d", subjectId, departmentId));

        // 1学科・クラス情報の取得
        List<Object[]> deptInfoList = departmentSubjectRepository.findCourseAndClass(departmentId);
        if (deptInfoList != null && !deptInfoList.isEmpty()) {

            // データが存在する場合、表示用文字列を作成
            Object[] row = deptInfoList.get(0);
            String courseName = (String) row[0];
            String className = (String) row[1];
            dto.setCourseAndGrade(String.format("%s %d年 %s", courseName, grade, className));
        } else {
            dto.setCourseAndGrade("-");
        }

        // 教科名の取得 & コマ数(RequiredCredits)の取得
        SubjectEntity subject = subjectRepository.findById(subjectId).orElse(null);
        if (subject != null) {
            dto.setSubjectName(subject.getSubjectName());
            dto.setCredits(subject.getRequiredCredits());
        } else {
            dto.setCredits(0);
        }
        
        // 担当教員の取得
        List<String> teacherNames = timetableRepository.findTeacherNameBySubjectAndClass(subjectId, departmentId);
        dto.setTeacherName(teacherNames != null && !teacherNames.isEmpty() ? teacherNames.get(0) : "未定");

        // スケジュール（曜日）の取得
        List<Object[]> scheduleData = timetableRepository.findScheduleAndRoom(subjectId, departmentId);
        
        Set<Integer> dayNumSet = new TreeSet<>();
        
        if (scheduleData != null) {
            for (Object[] row : scheduleData) {
                Object dateObj = row[0];
                if (dateObj != null) {
                    LocalDate date = null;
                    if (dateObj instanceof java.sql.Date) {
                        date = ((java.sql.Date) dateObj).toLocalDate();
                    } else if (dateObj instanceof java.time.LocalDate) {
                        date = (java.time.LocalDate) dateObj;
                    }
                    if (date != null) dayNumSet.add(date.getDayOfWeek().getValue());
                }
            }
        }
        
        List<String> dayStrings = new ArrayList<>();
        for (Integer dayNum : dayNumSet) {
            dayStrings.add(convertDayToKanji(dayNum));
        }
        
        dto.setSchedule(dayStrings.isEmpty() ? "未定" : String.join("、", dayStrings));

        // 履修学生リストの取得
        List<Object[]> studentData = enrollmentsRepository.findStudentIdAndNamesByClass(departmentId, grade);
        List<AdminSubjectInfoDto.StudentSimpleInfo> studentList = new ArrayList<>();
        
        if (studentData != null) {
            for (Object[] row : studentData) {
                Integer sId = (Integer) row[0];
                String sName = (String) row[1];
                studentList.add(new AdminSubjectInfoDto.StudentSimpleInfo(sId, sName));
            }
        }
        dto.setStudents(studentList);

        return dto;
    }

    //検索機能
    public List<AdminSubjectInfoDto.SubjectOption> searchSubjects(String query) {

        // キーワード検索条件を作成
        Specification<SubjectEntity> spec = searchService.createKeywordSpec(query, Arrays.asList("subjectName"));
        List<SubjectEntity> subjects = subjectRepository.findAll(spec);
        
        List<AdminSubjectInfoDto.SubjectOption> results = new ArrayList<>();
        List<DepartmentSubject> allRelations = departmentSubjectRepository.findAll(); 

        for (SubjectEntity s : subjects) {
            allRelations.stream()
                .filter(r -> r.getId().getSubjectId().equals(s.getSubjectId()))
                .forEach(match -> {
                    
                    // ★追加: 5つ目の引数(classDetail)を作成するロジック
                    String deptName = "";
                    String className = "";
                    if (match.getDepartment() != null) {
                        if (match.getDepartment().getMajor() != null) {
                            deptName = match.getDepartment().getMajor().getMajorName();
                        }
                        className = match.getDepartment().getClassName();
                    }
                    String detailText = String.format("%s %d年 %s", deptName, match.getGrade(), className);

                    // ★修正: 引数を5つ渡す (detailTextを追加)
                    results.add(new AdminSubjectInfoDto.SubjectOption(
                        s.getSubjectId(),
                        s.getSubjectName(),
                        match.getId().getDepartmentId(),
                        match.getGrade(),
                        detailText 
                    ));
                });
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