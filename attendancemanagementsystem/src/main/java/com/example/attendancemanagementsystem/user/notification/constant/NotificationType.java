package com.example.attendancemanagementsystem.user.notification.constant;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum NotificationType {

    // --- ID: 1 (メール専用: OTP) ---
    // グループ: SYSTEM
    PASSWORD_RESET_OTP(
        1,
        "【重要】パスワード再設定用認証コード", 
        "%s さん\n\n認証コード: %s\n有効期限: %d 分",
        "MAIL"
    ),

    // --- ID: 2 (完了通知) ---
    PASSWORD_CHANGE_COMPLETED(
        2,
        "パスワード変更完了のお知らせ",
        "%s さん\n\nパスワードの再設定が完了しました。",
        "MAIL"
    ),

    // --- ID: 3 (警告) ---
    ATTENDANCE_RISK_ALERT(
        3,
        "【警告】出席率低下のお知らせ: %s", 
        "%s さん\n\n科目「%s」の出席率が %d%% に低下しています。",
        "SYSTEM"
    ),

    // --- ID: 4 (メール専用: OTP) ---
    EMAIL_CHANGE_OTP(
        4,
        "【重要】メールアドレス変更確認", 
        "%s さん\n\n新しいメールアドレスの確認コード: %s\n有効期限: %d 分\n\n※心当たりがない場合は無視してください。",
        "MAIL"
    ),

    // --- ID: 5 (完了通知) ---
    EMAIL_CHANGE_COMPLETED(
        5,
        "メールアドレス変更完了のお知らせ", 
        "%s さん\n\nメールアドレスの変更手続きが完了しました。\n今後はこちらのメールアドレスでログインしてください。",
        "MAIL"
    ),
    
    // --- ID: 6 (公欠申請: 承認者向け) ---
    OFFICIAL_ABSENCE_REQUEST(
        6,
        "【公欠申請】申請が届きました", 
        "%s さんから公欠申請が提出されました。\n理由: %s\n\n詳細を確認して承認または却下を行ってください。",
        "REQUEST"
    ),

    // --- ID: 7 (申請許可: 生徒向け) ---
    REQUEST_APPROVED(
        7,
        "【申請結果】申請が承認されました",
        "%s さん\n\n提出した申請（%s）が承認されました。",
        "REQUEST"
    ),

    // --- ID: 8 (申請却下: 生徒向け) ---
    REQUEST_REJECTED(
        8,
        "【申請結果】申請が却下されました",
        "%s さん\n\n提出した申請（%s）が却下されました。\n理由: %s",
        "REQUEST"
    ),
    // --- ID: 9 (メール専用: ProfOTP) ---
    PASSWORD_CHANGE_OTP(
        9,
        "【重要】パスワード変更用認証コード", 
        "%s さん\n\nパスワード変更の手続きを受け付けました。\n以下の認証コードを入力してください。\n\n認証コード: %s\n有効期限: %d 分\n\n※心当たりがない場合は、速やかに管理者に連絡してください。",
        "MAIL"
    );

    private final int id;
    private final String subjectTemplate;
    private final String bodyTemplate;
    private final String groupCode;

    // コンストラクタ
    NotificationType(int id, String subjectTemplate, String bodyTemplate, String groupCode) {
        this.id = id;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.groupCode = groupCode;
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

    public String getGroupCode() {
        return groupCode;
    }

    // グループコードからIDリストを取得
    public static List<Integer> getIdsByGroup(String group) {
        return Arrays.stream(values())
                .filter(type -> type.getGroupCode().equalsIgnoreCase(group))
                .map(NotificationType::getId)
                .collect(Collectors.toList());
    }
}