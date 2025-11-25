package com.example.attendancemanagementsystem.attendance.record.controller;

import com.example.attendancemanagementsystem.attendance.record.dto.RecordDTO;
import com.example.attendancemanagementsystem.attendance.record.service.RecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance/record")

public class RecordController {
    private final RecordService recordService;
    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    // /api/attendance/record/nfc へのPOSTリクエストを処理
    @PostMapping("/nfc") 
    public ResponseEntity<String> recordFromNFC(@RequestBody RecordDTO recordDTO) {

        System.out.println("---- 受信テスト: NFCから送信されたJSON ----");
            System.out.println("card_id = " + recordDTO.getCardId());
            System.out.println("userId = " + recordDTO.getUserId());
            System.out.println("readTime" + recordDTO.getReadTime());
            System.out.println("reader_id = " + recordDTO.getReaderId());
            System.out.println("---------------------------------------------");


        try {
            // 処理をステップ5のServiceに丸投げ
            recordService.processAttendanceScan(recordDTO);

            // 成功したらGatewayに200 OKを返す
            return ResponseEntity.ok("Data processed.");
        } catch (Exception e) {
            System.err.println("出欠記録エラー: " + e.getMessage());
            return ResponseEntity.status(500).body("Error processing attendance data");
        }
    }
}
