package com.example.attendancemanagementsystem.user.admin.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
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

    @Transactional(readOnly = true)
    public ClassListDto getDailyClassInfo(String dateStr, String loginId, String searchWord) {
        ClassListDto dto = new ClassListDto();

        // 1. 日付のパース処理
        LocalDate date;
        try {
            if (dateStr != null && dateStr.matches("\\d{8}")) {
                date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } else if (dateStr != null && dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                date = LocalDate.parse(dateStr);
            } else {
                date = LocalDate.now();
            }
        } catch (Exception e) {
            date = LocalDate.now();
        }

        // 2. DTOへのセット
        dto.setSearchDate(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        dto.setHeaderDate(date.format(DateTimeFormatter.ofPattern("yyyy年 M月 d日 (E)", Locale.JAPANESE)));
        dto.setSearchWord(searchWord);

        // 3. ユーザー情報の取得
        UsersEntity user = usersRepository.findByLoginId(loginId).orElse(null);

        // 4. 全時限枠の準備
        List<TimeSlotEntity> allSlots = timeSlotRepository.findAllByOrderBySlotIdAsc();
        Map<Integer, PeriodDetail> periodMap = new LinkedHashMap<>();
        
        for (TimeSlotEntity slot : allSlots) {
            PeriodDetail detail = new PeriodDetail();
            detail.setHasClass(false);
            periodMap.put(slot.getSlotId(), detail);
        }

        if (user == null) {
            dto.setPeriods(periodMap);
            return dto;
        }

        // =========================================================
        // 5. Specificationを使った検索
        // =========================================================
        
        // ★修正ポイント: ラムダ式の中で使うために、finalな変数に値をコピーする
        final LocalDate searchDate = date;

        // (A) 基本条件: ユーザーID AND 日付
        Specification<SessionEntity> spec = Specification.where((root, query, cb) -> {
            return cb.and(
                cb.equal(root.get("userId"), user.getUserId()),
                cb.equal(root.get("sessionDate"), searchDate) // ★コピーした変数(searchDate)を使う
            );
        });

        // (B) キーワード検索条件 (入力がある場合のみ追加)
        if (searchWord != null && !searchWord.isEmpty()) {
            List<String> targetColumns = Arrays.asList("subject.subjectName", "classroom.classroomName");
            Specification<SessionEntity> wordSpec = searchService.createKeywordSpec(searchWord, targetColumns);
            spec = spec.and(wordSpec);
        }

        // (C) 検索実行
        List<SessionEntity> sessions = sessionRepository.findAll(spec);

        // =========================================================

        // 6. 結果のマッピング
        for (SessionEntity s : sessions) {
            if (s.getTimeSlot() == null) continue;

            int p = s.getTimeSlot().getSlotId();

            if (periodMap.containsKey(p)) {
                PeriodDetail detail = periodMap.get(p);
                detail.setHasClass(true);

                if (s.getSubject() != null) {
                    detail.setSubjectName(s.getSubject().getSubjectName());
                    detail.setSubjectId(s.getSubject().getSubjectId());
                } else {
                    detail.setSubjectName("教科未登録");
                    detail.setSubjectId(null);
                }

                if (s.getClassroom() != null) {
                    detail.setClassroomName(s.getClassroom().getClassroomName());
                } else {
                    detail.setClassroomName("-");
                }
            }
        }

        dto.setPeriods(periodMap);
        return dto;
    }
}