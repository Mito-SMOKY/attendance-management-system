package com.example.attendancemanagementsystem.user.admin.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.SessionEntity;
import com.example.attendancemanagementsystem.common.entity.TimeSlotEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.SessionRepository;
import com.example.attendancemanagementsystem.common.repository.TimeSlotRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.common.service.SearchService;
import com.example.attendancemanagementsystem.user.admin.dto.ClassListDto;
import com.example.attendancemanagementsystem.user.admin.dto.ClassListDto.PeriodDetail;


@Service
public class AdminClassListService {

    private final SessionRepository sessionRepository;
    private final UsersRepository usersRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final SearchService searchService;

    public AdminClassListService(SessionRepository sessionRepository, 
                                UsersRepository usersRepository,
                                TimeSlotRepository timeSlotRepository,
                                SearchService searchService) {
        this.sessionRepository = sessionRepository;
        this.usersRepository = usersRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.searchService = searchService;
    }

    // 授業一覧データを取得する
    @Transactional(readOnly = true)
    public ClassListDto getDailyClassInfo(String dateStr, String loginId, String searchWord) {
        ClassListDto dto = new ClassListDto();

        // 文字列の日付を変換する処理
        LocalDate date;
        try {
            if (dateStr != null && dateStr.matches("\\d{8}")) {

                // yyyyMMdd形式の場合
                date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } else if (dateStr != null && dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {

                // yyyy-MM-dd形式の場合
                date = LocalDate.parse(dateStr);
            } else {

                // 形式が合わない場合は今日の日付にする
                date = LocalDate.now();
            }
        } catch (Exception e) {

            // エラーが発生した場合も今日の日付にする
            date = LocalDate.now();
        }

        // DTOにセットする
        dto.setSearchDate(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        dto.setHeaderDate(date.format(DateTimeFormatter.ofPattern("yyyy年 M月 d日 (E)", Locale.JAPANESE)));
        dto.setSearchWord(searchWord);

        // ログインIDからユーザー情報を取得
        UsersEntity user = usersRepository.findByLoginId(loginId).orElse(null);

        // 全ての時限の枠を用意する
        List<TimeSlotEntity> allSlots = timeSlotRepository.findAllByOrderBySlotIdAsc();
        Map<Integer, PeriodDetail> periodMap = new LinkedHashMap<>();
        
        // 全時限分の空データを一旦作成してマップに入れる
        for (TimeSlotEntity slot : allSlots) {
            PeriodDetail detail = new PeriodDetail();
            detail.setHasClass(false);
            periodMap.put(slot.getSlotId(), detail);
        }

        // ユーザーが存在しない場合は、空の表だけ返して終了する
        if (user == null) {
            dto.setPeriods(periodMap);
            return dto;
        }

        
        //日付検索
        List<SessionEntity> sessions = sessionRepository.findByUserIdAndDate(user.getUserId(), date);

        // 取得した授業データを、時限ごとのマップに当てはめる
        for (SessionEntity s : sessions) {

            // 時限情報がないデータはスキップする
            if (s.getTimeSlot() == null) continue;

            int p = s.getTimeSlot().getSlotId();

            // 用意しておいたマップに該当する時限があればデータをセット
            if (periodMap.containsKey(p)) {
                PeriodDetail detail = periodMap.get(p);
                detail.setHasClass(true);
                detail.setSessionId(s.getSessionId());

                // 科目名のセット
                if (s.getSubject() != null) {
                    detail.setSubjectName(s.getSubject().getSubjectName());
                    detail.setSubjectId(s.getSubject().getSubjectId());
                } else {
                    detail.setSubjectName("教科未登録");
                    detail.setSubjectId(null);
                }

                // 教室名のセット
                if (s.getClassroom() != null) {
                    detail.setClassroomName(s.getClassroom().getClassroomName());
                } else {
                    detail.setClassroomName("-");
                }
            }
        }

        // DTOにセット
        dto.setPeriods(periodMap);
        return dto;
    }
}