package com.example.attendancemanagementsystem.user.student.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.common.entity.RequestEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.RequestRepository;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final UsersRepository usersRepository;

    public RequestService(RequestRepository requestRepository, UsersRepository usersRepository) {
        this.requestRepository = requestRepository;
        this.usersRepository = usersRepository;
    }

    //承認者候補（管理者）のリストを取得する
    public List<UsersEntity> getAllAdmins() {
        return usersRepository.findByUserTypeId(2); 
    }

    /**
     * 公欠申請を作成・保存する
     * @param studentId 申請者(生徒)ID
     * @param start     開始日
     * @param end       終了日
     * @param periods   時限リスト
     * @param reason    理由
     * @param approverId 承認者ID
     */
    @Transactional
    public void createOfficialAbsenceRequest(Integer studentId, LocalDate start, LocalDate end, List<Integer> periods, String reason, Integer approverId) {
        
        UsersEntity student = usersRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("生徒が見つかりません: " + studentId));

        RequestEntity request = new RequestEntity();
        request.setRequestTypeId(1); // 1: 公欠申請
        request.setRequesterUserId(studentId);
        request.setRequestMessage(reason);
        request.setStatus(0); // 0: 申請中
        
        // 承認者のセット
        if (approverId == null) {
            throw new IllegalArgumentException("承認者が選択されていません");
        }
        request.setApproverId(approverId);
        
        request.setStartDate(start);
        request.setEndDate(end);
        
        // 時限リストをカンマ区切り文字列に変換
        String periodsStr = periods.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
        request.setPeriods(periodsStr);

        // 公欠申請の対象は自分自身
        request.addTargetUser(student);

        requestRepository.save(request);
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

        // ステータスが「0: 申請中」以外ならエラー
        Integer currentStatus = request.getStatus() != null ? request.getStatus() : 0;
        if (currentStatus != 0) {
            throw new IllegalStateException("既に処理済みのため取り下げできません");
        }

        request.setStatus(3); // 3: 取り下げ
        requestRepository.save(request);
    }

    //自分の申請履歴を取得する
    public List<RequestEntity> getMyRequestHistory(Integer userId) {
        return requestRepository.findByRequesterUserIdOrderByCreatedAtDesc(userId);
    }
}