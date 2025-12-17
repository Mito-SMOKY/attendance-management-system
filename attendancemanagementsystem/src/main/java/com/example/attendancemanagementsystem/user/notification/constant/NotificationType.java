package com.example.attendancemanagementsystem.user.notification.constant;

public enum NotificationType {

    // ID: 1 (メール専用)
    PASSWORD_RESET_OTP(
        1,
        "【重要】パスワード再設定用認証コード", 
        "%s さん\n\n認証コード: %s\n有効期限: %d 分"
    ),

    // ID: 2
    PASSWORD_CHANGE_COMPLETED(
        2,
        "パスワード変更完了のお知らせ",
        "%s さん\n\nパスワードの再設定が完了しました。"
    ),

    // ID: 3
    ATTENDANCE_RISK_ALERT(
        3,
        "【警告】出席率低下のお知らせ: %s", 
        "%s さん\n\n科目「%s」の出席率が %d%% に低下しています。"
    );

    // DB保存用のIDを追加
    private final int id;
    private final String subjectTemplate;
    private final String bodyTemplate;

    NotificationType(int id, String subjectTemplate, String bodyTemplate) {
        this.id = id;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
    }

    public int getId() {
        return id; 
    }

    public String getSubjectTemplate() { 
        return subjectTemplate; 
    }

    public String getBodyTemplate() { 
        return bodyTemplate; 
    }
}