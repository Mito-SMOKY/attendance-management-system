package com.example.attendancemanagementsystem.user.student.service;

import java.time.DayOfWeek; // 追加
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.RequestDetailEntity;
import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import com.example.attendancemanagementsystem.user.notification.service.NotificationMessageService;

@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final UsersRepository usersRepository;
    private final NotificationMessageService notificationMessageService;

    public RequestService(RequestRepository requestRepository, UsersRepository usersRepository, NotificationMessageService notificationMessageService) {
        this.requestRepository = requestRepository;
        this.usersRepository = usersRepository;
        this.notificationMessageService = notificationMessageService;
    }

    //承認者候補（管理者）のリストを取得する
    public List<UsersEntity> getAllAdmins() {
        return usersRepository.findByUserTypeId(2); 
    }

    /**
     * 公欠申請を作成・保存する
     */
    @Transactional
    public void createOfficialAbsenceRequest(Integer approverId, UsersEntity studentUser, LocalDate start, LocalDate end, List<Integer> periods, String reason) {
        
        UsersEntity approver = usersRepository.findById(approverId)
                .orElseThrow(() -> new IllegalArgumentException("承認者が見つかりません"));

        RequestEntity request = new RequestEntity();
        request.setRequestTypeId(1); // 1: 公欠申請
        request.setRequesterUserId(studentUser.getUserId());
        request.setRequestMessage(reason);
        request.setStatus(1); // 1: 承認待ち
        request.setApproverId(approverId);
        
        request.setStartDate(start);
        request.setEndDate(end);
        
        // 時限リストをカンマ区切り文字列に変換
        String periodsStr = periods.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
        request.setPeriods(periodsStr);

        // 公欠申請の対象は自分自身
        request.addTargetUser(studentUser);
        
        // ▼▼▼ RequestDetail (詳細データ) の作成処理 ▼▼▼
        List<RequestDetailEntity> details = new ArrayList<>();
        
        // 開始日から終了日までの各日付についてループ
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            
            // ▼▼▼ 土日の場合はスキップ ▼▼▼
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                currentDate = currentDate.plusDays(1);
                continue;
            }
            // ▲▲▲ スキップ処理ここまで ▲▲▲

            // 選択された各時限についてデータを作成
            for (Integer slotId : periods) {
                RequestDetailEntity detail = new RequestDetailEntity();
                detail.setRequest(request);      // 親となる申請をセット
                detail.setTargetDate(currentDate); // 日付
                detail.setSlotId(slotId);        // 時限
                
                details.add(detail);
            }
            
            // 次の日に進める
            currentDate = currentDate.plusDays(1);
        }
        
        // 親Entityに子Entityのリストをセット
        // (RequestEntityに CascadeType.ALL が設定されているため、親を保存すれば子も保存されます)
        request.setRequestDetails(details);

        requestRepository.save(request);
        
        // 通知送信
        notificationMessageService.createOfficialAbsenceRequestNotification(approver, studentUser, reason);
    }

    //申請を取り下げる (ソフトデリート)
    @Transactional
    public void withdrawRequest(Integer requestId, Integer userId) {
        RequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("該当する申請が見つかりません"));

        // 本人の申請かチェック
        if (!request.getRequesterUserId().equals(userId)) {
            throw new SecurityException("他人の申請は操作できません");
        }

        // ステータスが「1: 承認待ち」以外ならエラー
        Integer currentStatus = request.getStatus() != null ? request.getStatus() : 0;
        if (currentStatus != 1) { 
            throw new IllegalStateException("既に処理済みのため取り下げできません");
        }

        request.setStatus(3); // 3: 取り下げ
        requestRepository.save(request);
    }

    // 自分の申請履歴を取得
    public List<RequestEntity> getMyRequestHistory(Integer userId) {
        return requestRepository.findByRequesterUserIdOrderByCreatedAtDesc(userId);
    }
}