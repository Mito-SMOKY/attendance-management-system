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
import com.example.attendancemanagementsystem.user.admin.dto.ClassListDto;
import com.example.attendancemanagementsystem.user.admin.dto.ClassListDto.PeriodDetail;

@Service
public class AdminClassListService {

    private final SessionRepository sessionRepository;
    private final UsersRepository usersRepository;
    private final TimeSlotRepository timeSlotRepository;

    public AdminClassListService(SessionRepository sessionRepository, 
                                UsersRepository usersRepository,
                                TimeSlotRepository timeSlotRepository) {
        this.sessionRepository = sessionRepository;
        this.usersRepository = usersRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    /**
     * 指定された日付とログインIDに基づき、管理者用の授業一覧データを取得・生成します。
     * * @param dateStr 検索対象日付 (yyyyMMdd 形式 または yyyy-MM-dd 形式)
     * @param loginId 教員のログインID
     * @return 画面表示用DTO
     */
    @Transactional(readOnly = true)
    public ClassListDto getDailyClassInfo(String dateStr, String loginId) {
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

        // 2. 画面表示用・検索用日付文字列のセット
        dto.setSearchDate(date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        dto.setHeaderDate(date.format(DateTimeFormatter.ofPattern("yyyy年 M月 d日 (E)", Locale.JAPANESE)));

        // 3. ログインIDからユーザー情報を取得
        UsersEntity user = usersRepository.findByLoginId(loginId).orElse(null);

        // 4. 全時限（1限〜N限）をDBから取得
        //    ユーザーが存在しない場合でも、空の表を表示するために時限枠は取得する
        List<TimeSlotEntity> allSlots = timeSlotRepository.findAllByOrderBySlotIdAsc();
        
        // 順序を保持するために LinkedHashMap を使用
        Map<Integer, PeriodDetail> periodMap = new LinkedHashMap<>();
        
        // まず全時限の空枠を作る
        for (TimeSlotEntity slot : allSlots) {
            PeriodDetail detail = new PeriodDetail();
            detail.setHasClass(false);
            periodMap.put(slot.getSlotId(), detail);
        }

        // ユーザーが見つからない場合はここで空枠のみ返却
        if (user == null) {
            dto.setPeriods(periodMap);
            return dto;
        }

        // 5. DBから授業データを取得
        List<SessionEntity> sessions = sessionRepository.findByUserIdAndDate(user.getUserId(), date);

        // 6. 授業データを時限マップにマッピング
        for (SessionEntity s : sessions) {
            if (s.getTimeSlot() == null) {
                continue;
            }

            int p = s.getTimeSlot().getSlotId();

            if (periodMap.containsKey(p)) {
                PeriodDetail detail = periodMap.get(p);
                detail.setHasClass(true);

                // 教科名の設定
                if (s.getSubject() != null) {
                    detail.setSubjectName(s.getSubject().getSubjectName());
                    detail.setSubjectId(s.getSubject().getSubjectId());
                } else {
                    detail.setSubjectName("教科未登録");
                    detail.setSubjectId(null);
                }

                // 教室名の設定
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