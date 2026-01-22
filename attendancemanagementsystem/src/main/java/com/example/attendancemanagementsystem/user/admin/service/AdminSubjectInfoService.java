package com.example.attendancemanagementsystem.user.admin.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import com.example.attendancemanagementsystem.common.repository.SubjectFacultyRepository;
import com.example.attendancemanagementsystem.common.repository.SubjectRepository;
import com.example.attendancemanagementsystem.common.repository.TimetableRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.common.service.SearchService; 
import com.example.attendancemanagementsystem.user.admin.dto.AdminSubjectInfoDto;

@Service
@Transactional(readOnly = true)
public class AdminSubjectInfoService {

    private final DepartmentSubjectRepository departmentSubjectRepository;
    private final TimetableRepository timetableRepository;
    private final SubjectRepository subjectRepository;
    private final EnrollmentsRepository enrollmentsRepository;
    private final SubjectFacultyRepository subjectFacultyRepository; 
    private final UsersRepository usersRepository; 
    private final SearchService searchService; 

    public AdminSubjectInfoService(
            DepartmentSubjectRepository departmentSubjectRepository,
            TimetableRepository timetableRepository,
            SubjectRepository subjectRepository,
            EnrollmentsRepository enrollmentsRepository,
            SubjectFacultyRepository subjectFacultyRepository, 
            UsersRepository usersRepository, 
            SearchService searchService) {
        this.departmentSubjectRepository = departmentSubjectRepository;
        this.timetableRepository = timetableRepository;
        this.subjectRepository = subjectRepository;
        this.enrollmentsRepository = enrollmentsRepository;
        this.subjectFacultyRepository = subjectFacultyRepository; 
        this.usersRepository = usersRepository; 
        this.searchService = searchService;
    }

    // 教科詳細画面を表示
    public AdminSubjectInfoDto getSubjectInfo(Integer departmentId, Integer subjectId, Integer grade) {

        // IDや年度情報をセット
        AdminSubjectInfoDto dto = new AdminSubjectInfoDto();
        dto.setDepartmentId(departmentId);
        dto.setSubjectId(subjectId);
        dto.setGrade(grade);
        dto.setFiscalYear(calculateFiscalYear());
        dto.setDisplaySubjectId(String.format("%04d-%02d", subjectId, departmentId));

        // コース名とクラス名を取得
        List<Object[]> deptInfoList = departmentSubjectRepository.findCourseAndClass(departmentId);
        
        // 学科情報がある場合は整形してDTOにセット、なければハイフンを設定
        if (deptInfoList != null && !deptInfoList.isEmpty()) {
            Object[] row = deptInfoList.get(0);
            String courseName = (String) row[0];
            String className = (String) row[1];
            dto.setCourseAndGrade(String.format("%s %d年 %s", courseName, grade, className));
        } else {
            dto.setCourseAndGrade("-");
        }

        // 教科情報を取得し存在すれば名前と単位数をセット
        SubjectEntity subject = subjectRepository.findById(subjectId).orElse(null);
        if (subject != null) {
            dto.setSubjectName(subject.getSubjectName());
            dto.setCredits(subject.getRequiredCredits());
        } else {
            dto.setCredits(0);
        }
        
        // 担当教員の取得
        Integer teacherId = subjectFacultyRepository.findTeacherIdBySubjectId(subjectId);
        String teacherName = "未定";
        
        // 氏名を取得
        if (teacherId != null) {
            String name = usersRepository.findNameByUserId(teacherId);
            if (name != null) {
                teacherName = name;
            }
        }
        dto.setTeacherName(teacherName);

        // 対象教科の日付・教室情報を取得
        List<Object[]> scheduleData = timetableRepository.findScheduleAndRoom(subjectId, departmentId);
        
        // 曜日の数値を格納
        Set<Integer> dayNumSet = new TreeSet<>();
        
        // 曜日をセット
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
        
        // 日本語表記リストに変換
        List<String> dayStrings = new ArrayList<>();
        for (Integer dayNum : dayNumSet) {
            dayStrings.add(convertDayToKanji(dayNum));
        }
        
        // 曜日リストをカンマ区切りで結合し、空の場合は「未定」とする
        dto.setSchedule(dayStrings.isEmpty() ? "未定" : String.join("、", dayStrings));

        // 学科・学年の履修学生リストを取得
        List<Object[]> studentData = enrollmentsRepository.findStudentIdAndNamesByClass(departmentId, grade);
        List<AdminSubjectInfoDto.StudentSimpleInfo> studentList = new ArrayList<>();
        
        // 学生データをDtoに追加
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

    // キーワード検索
    public List<AdminSubjectInfoDto.SubjectOption> searchSubjects(String query) {

        // 教科名を対象とした部分一致検索の条件を作成
        Specification<SubjectEntity> spec = searchService.createKeywordSpec(query, Arrays.asList("subjectName"));
        List<SubjectEntity> subjects = subjectRepository.findAll(spec);
        
        // 全データを取得
        List<AdminSubjectInfoDto.SubjectOption> results = new ArrayList<>();
        List<DepartmentSubject> allRelations = departmentSubjectRepository.findAll(); 

        // 紐づく学科や学年情報をストリーム処理で検索
        for (SubjectEntity s : subjects) {
            allRelations.stream()
                .filter(r -> r.getId().getSubjectId().equals(s.getSubjectId()))
                .forEach(match -> {
                    
                    // 学科名とクラス名を抽出して表示用文字列を作成
                    String deptName = "";
                    String className = "";
                    if (match.getDepartment() != null) {
                        if (match.getDepartment().getMajor() != null) {
                            deptName = match.getDepartment().getMajor().getMajorName();
                        }
                        className = match.getDepartment().getClassName();
                    }
                    String detailText = String.format("%s %d年 %s", deptName, match.getGrade(), className);

                    // 検索結果用DTOを作成しリストに追加
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

    // 日本語の曜日文字列に変換
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

    // 現在日付から年度（4月始まり）を計算して文字列で返す
    private String calculateFiscalYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        // 1月～3月の場合は前年度として扱う
        if (now.getMonthValue() < 4) year -= 1;
        return year + "年度";
    }
}