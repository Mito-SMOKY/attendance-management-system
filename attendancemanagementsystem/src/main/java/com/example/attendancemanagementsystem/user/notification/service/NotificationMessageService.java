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

    // 自動通知用ID
    private static final int SYSTEM_SENDER_ID = 1;

    //パスワード変更完了通知
    @Transactional
    public void createPasswordChangeCompletion(UsersEntity user) {
        create(user, SYSTEM_SENDER_ID, NotificationType.PASSWORD_CHANGE_COMPLETED, null, user.getName());
    }

    // メールアドレス変更完了通知
    @Transactional
    public void createEmailChangeCompletion(UsersEntity user) {
        create(user, SYSTEM_SENDER_ID, NotificationType.EMAIL_CHANGE_COMPLETED, null, 
            user.getName());
    }

    // 出席率アラート通知
    @Transactional
    public void createAttendanceRisk(UsersEntity student, SubjectListDto dto) {
        String subjectName = dto.getSubjectName();
        int rate = (int)(dto.getAttendanceRate() * 100);
        create(student, SYSTEM_SENDER_ID, NotificationType.ATTENDANCE_RISK_ALERT, subjectName, 
            student.getName(), subjectName, rate);
    }

    // 検証用出席率アラート通知
    @Transactional
    public void createAttendanceRiskSimple(UsersEntity student, String subjectName, double attendanceRate) {
        int ratePercent = (int)(attendanceRate * 100);
        create(student, SYSTEM_SENDER_ID, NotificationType.ATTENDANCE_RISK_ALERT, subjectName, 
            student.getName(), subjectName, ratePercent);
    }
    
    // 共通保存処理
    private void create(UsersEntity receiver, int senderId, NotificationType type, String titleArg, Object... bodyArgs) {
        try {
            // 本文の作成
            String message = String.format(type.getBodyTemplate(), bodyArgs);

            NotificationEntity entity = new NotificationEntity();
            
            // 必須フィールドの設定
            entity.setReceiverUserId(receiver.getUserId()); // 受信者
            entity.setSenderUserId(senderId);               // 送信者
            entity.setNotificationTypeId(type.getId());     // タイプID
            entity.setMessage(message);                     // 本文
            
            notificationRepository.save(entity);
            
            System.out.println("通知保存完了: From=" + senderId + " To=" + receiver.getUserId() + " Type=" + type.getId());

        } catch (Exception e) {
            System.err.println("通知保存エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
}