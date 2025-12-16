package com.example.attendancemanagementsystem.user.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendancemanagementsystem.attendance.display.dto.SubjectListDto;
import com.example.attendancemanagementsystem.common.entity.NotificationEntity;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.NotificationRepository;
import com.example.attendancemanagementsystem.user.notification.constant.NotificationType;

@Service
public class NotificationMessageService {

    @Autowired
    private NotificationRepository notificationRepository;

    // システム通知の送り主ID (仮に 1:管理者 とします)
    private static final int SYSTEM_SENDER_ID = 1;

    // パスワード変更完了のお知らせ
    @Transactional
    public void createPasswordChangeCompletion(UsersEntity user) {
        create(user, NotificationType.PASSWORD_CHANGE_COMPLETED, null, user.getName());
    }

    // 出席率アラート
    @Transactional
    public void createAttendanceRisk(UsersEntity student, SubjectListDto dto) {
        String subjectName = dto.getSubjectName();
        int rate = (int)(dto.getAttendanceRate() * 100);

        create(student, NotificationType.ATTENDANCE_RISK_ALERT, subjectName, 
            student.getName(), subjectName, rate);
    }

    // 共通保存処理
    private void create(UsersEntity receiver, NotificationType type, String titleArg, Object... bodyArgs) {
        try {
            String message = String.format(type.getBodyTemplate(), bodyArgs);

            NotificationEntity entity = new NotificationEntity();
            
            // 必須フィールドの設定
            entity.setReceiverUserId(receiver.getUserId()); // 受信者
            entity.setSenderUserId(SYSTEM_SENDER_ID);       // 送信者
            entity.setNotificationTypeId(type.getId());     // タイプID
            entity.setMessage(message);                     // 本文
            notificationRepository.save(entity);
            
            System.out.println("通知保存完了: To=" + receiver.getUserId() + " Type=" + type.getId());

        } catch (Exception e) {
            System.err.println("通知保存エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
}