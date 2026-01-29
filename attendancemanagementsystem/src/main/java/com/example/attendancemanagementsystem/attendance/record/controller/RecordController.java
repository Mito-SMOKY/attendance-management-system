package com.example.attendancemanagementsystem.attendance.record.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendancemanagementsystem.attendance.record.dto.RecordDTO;
import com.example.attendancemanagementsystem.attendance.record.dto.VerifiedRecordDto;
import com.example.attendancemanagementsystem.attendance.record.service.RecordService;
import com.example.attendancemanagementsystem.common.component.NfcDataHolder;

@RestController
@RequestMapping("/api/attendance/record")
public class RecordController {

    private final RecordService recordService;
    private final NfcDataHolder nfcDataHolder;

    // コンストラクタ
    public RecordController(RecordService recordService, NfcDataHolder nfcDataHolder) {
        this.recordService = recordService;
        this.nfcDataHolder = nfcDataHolder;
    }

    // NFC出欠記録受信エンドポイント
    @PostMapping("/nfc")
    public ResponseEntity<String> recordFromNFC(@RequestBody RecordDTO recordDTO) {
        try {

            //検証実行
            VerifiedRecordDto verifiedData = recordService.processAttendanceScan(recordDTO);

            //成功 -> 画面に通知 (既存カードとしてIDを表示)
            nfcDataHolder.setScannedData(
                verifiedData.getCardId(), 
                String.valueOf(verifiedData.getUserId())
            );
            return ResponseEntity.ok("Detected (Registered)");

        } catch (IllegalArgumentException | IllegalStateException e) {
            
            //検証エラー (未登録など) 画面通知
            String cardId = recordDTO.getCardId();
            if (cardId != null && !cardId.isEmpty()) {

                // 新規カードとして書き込み画面へ 
                nfcDataHolder.setScannedData(cardId, null);
                return ResponseEntity.ok("Detected (New Card)");
            }
            
            // カードIDすら読めない -> 「読み込み失敗画面」へ
            nfcDataHolder.setError("カード情報の読み取りに失敗しました");
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());

        } catch (Exception e) {
            // その他のシステムエラー -> 「読み込み失敗画面」へ
            nfcDataHolder.setError("システムエラー: " + e.getMessage());
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}