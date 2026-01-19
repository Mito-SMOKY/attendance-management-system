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

            // データがない場合はハイフンを表示
            dto.setCourseAndGrade("-");
        }

        // 教科名の取得
        SubjectEntity subject = subjectRepository.findById(subjectId).orElse(null);
        if (subject != null) {
            dto.setSubjectName(subject.getSubjectName());
        }
        
        // 担当教員の取得
        List<String> teacherNames = timetableRepository.findTeacherNameBySubjectAndClass(subjectId, departmentId);

        // リストが空でなければ最初の名前を表示、なければ"未定"とする
        dto.setTeacherName(teacherNames != null && !teacherNames.isEmpty() ? teacherNames.get(0) : "未定");

        // スケジュール（曜日・教室）の取得
        List<Object[]> scheduleData = timetableRepository.findScheduleAndRoom(subjectId, departmentId);
        
        // 重複を排除し、曜日順に並べるためにTreeSetを使用
        Set<Integer> dayNumSet = new TreeSet<>();

        // 教室名の重複を排除するためにHashSetを使用
        Set<String> roomSet = new HashSet<>();
        
        if (scheduleData != null) {

            // 合計コマ数を設定
            dto.setTotalClasses(scheduleData.size() + "コマ");
            
            for (Object[] row : scheduleData) {
                Object dateObj = row[0];
                String room = (String) row[1];
                
                // 日付オブジェクトから曜日数値を取得
                if (dateObj != null) {
                    LocalDate date = null;
                    if (dateObj instanceof java.sql.Date) {
                        date = ((java.sql.Date) dateObj).toLocalDate();
                    } else if (dateObj instanceof java.time.LocalDate) {
                        date = (java.time.LocalDate) dateObj;
                    }
                    // 日付が取得できたら曜日をセット
                    if (date != null) dayNumSet.add(date.getDayOfWeek().getValue());
                }

                // 教室名があればセットに追加
                if (room != null && !room.isEmpty()) roomSet.add(room);
            }
        }
        
        // 曜日数値を日本語文字列に変換
        List<String> dayStrings = new ArrayList<>();
        for (Integer dayNum : dayNumSet) {
            dayStrings.add(convertDayToKanji(dayNum));
        }
        
        // リストを結合して表示用文字列を作成
        dto.setSchedule(dayStrings.isEmpty() ? "未定" : String.join("、", dayStrings));
        dto.setClassroom(roomSet.isEmpty() ? "未定" : String.join(", ", roomSet));

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
        
        // 条件に合致する教科をDBから全件取得
        List<SubjectEntity> subjects = subjectRepository.findAll(spec);
        
        // 結果格納用リスト
        List<AdminSubjectInfoDto.SubjectOption> results = new ArrayList<>();
        
        // 遷移先URLを作るために、教科と学科の紐付けテーブルを全件取得
        List<DepartmentSubject> allRelations = departmentSubjectRepository.findAll(); 

        // 検索された各教科について、紐付いている学科・学年を探す
        for (SubjectEntity s : subjects) {

            // 教科IDに一致する紐付けデータを最初の一つだけ取得
            DepartmentSubject match = allRelations.stream()
                .filter(r -> r.getId().getSubjectId().equals(s.getSubjectId()))
                .findFirst().orElse(null);

            // 紐付けが見つかった場合のみ、検索結果として追加
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

    // 曜日の数値を漢字に変換するヘルパーメソッド
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

    // 現在日付から年度（4月始まり）を計算するヘルパーメソッド
    private String calculateFiscalYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        
        // 1月〜3月の場合は前年度として扱う
        if (now.getMonthValue() < 4) year -= 1;
        return year + "年度";
    }
}