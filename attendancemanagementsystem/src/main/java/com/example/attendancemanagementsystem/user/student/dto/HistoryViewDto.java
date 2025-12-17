package com.example.attendancemanagementsystem.user.student.dto;

import java.time.format.DateTimeFormatter;
import com.example.attendancemanagementsystem.common.entity.RequestEntity;

// 申請履歴画面表示用のデータクラス
public class HistoryViewDto {
    
    private Integer requestId;
    private String requestDate;
    private String type;
    private String targetDate;
    private String periods;
    private String status;

    // コンストラクタ
    public HistoryViewDto(Integer requestId, String requestDate, String type, String targetDate, String periods, String status) {
        this.requestId = requestId;
        this.requestDate = requestDate;
        this.type = type;
        this.targetDate = targetDate;
        this.periods = periods;
        this.status = status;
    }

    // EntityからDTOへ変換する静的メソッド
    public static HistoryViewDto fromEntity(RequestEntity entity) {
        Integer id = entity.getRequestId();

        // 1. 申請日フォーマット
        String dateStr = "";
        if (entity.getCreatedAt() != null) {
            dateStr = entity.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        }
        
        // 2. 申請種別の判定 (1=公欠)
        String typeName = (entity.getRequestTypeId() != null && entity.getRequestTypeId() == 1) ? "公欠" : "その他";

        // 3. 対象日付の作成
        String targetDateStr = "";
        if (entity.getStartDate() != null) {
            targetDateStr = entity.getStartDate().format(DateTimeFormatter.ofPattern("MM/dd"));
            if (entity.getEndDate() != null && !entity.getEndDate().equals(entity.getStartDate())) {
                targetDateStr += " ～ " + entity.getEndDate().format(DateTimeFormatter.ofPattern("MM/dd"));
            }
        }

        // 4. 時限情報の作成
        String periodsStr = "-";
        if (entity.getPeriods() != null && !entity.getPeriods().isEmpty()) {
            periodsStr = entity.getPeriods().replace(",", "・") + "限";
        }

        // 5. ステータスの判定
        String statusStr;
        Integer statusVal = entity.getStatus() != null ? entity.getStatus() : 0;
        switch (statusVal) {
            case 1: statusStr = "承認済み"; break;
            case 2: statusStr = "却下"; break;
            case 3: statusStr = "取消済み"; break;
            default: statusStr = "申請中"; break;
        }

        return new HistoryViewDto(id, dateStr, typeName, targetDateStr, periodsStr, statusStr);
    }

    // --- Getter ---
    public Integer getRequestId() { return requestId; }
    public String getRequestDate() { return requestDate; }
    public String getType() { return type; }
    public String getTargetDate() { return targetDate; }
    public String getPeriods() { return periods; }
    public String getStatus() { return status; }
}