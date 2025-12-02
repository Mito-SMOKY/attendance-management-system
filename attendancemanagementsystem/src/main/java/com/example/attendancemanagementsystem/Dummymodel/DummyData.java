package com.example.attendancemanagementsystem.Dummymodel;

public class DummyData {

    private final String period; // 時限 (例: "1限")
    private final String subject; // 授業名 (例: "ソフトウェア開発論")
    private final String status; // 出欠 (例: "出席", "遅刻")

    // コンストラクタ
    public DummyData(String period, String subject, String status) {
        this.period = period;
        this.subject = subject;
        this.status = status;
    }

    // Getterメソッド (Thymeleafでアクセスするために必須)
    public String getPeriod() {
        return period;
    }

    public String getSubject() {
        return subject;
    }

    public String getStatus() {
        return status;
    }

}