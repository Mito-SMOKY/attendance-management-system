package com.example.attendancemanagementsystem.attendance.writer.controller;

import com.example.attendancemanagementsystem.attendance.writer.dto.WriterDto;
import com.example.attendancemanagementsystem.attendance.writer.service.WriterService;
import com.example.attendancemanagementsystem.common.component.NfcDataHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/issue")
public class WriterController {

    private final WriterService writerService;
    private final NfcDataHolder nfcDataHolder;

    // コンストラクタ
    public WriterController(WriterService writerService, NfcDataHolder nfcDataHolder) {
        this.writerService = writerService;
        this.nfcDataHolder = nfcDataHolder;
    }

    //ポーリング(問い合わせ)に応答する ---
    @GetMapping("/poll")
    public ResponseEntity<Map<String, Object>> pollStatus() {
        Map<String, Object> response = new HashMap<>();
        
        // 最新の状態を返す
        response.put("status", nfcDataHolder.getCurrentStatus());
        response.put("timestamp", nfcDataHolder.getTimestamp());
        response.put("error", nfcDataHolder.getErrorMessage());
        
        // もし検知済み(SCANNED)なら、ID情報も一緒に返す
        if ("SCANNED".equals(nfcDataHolder.getCurrentStatus())) {
            response.put("cardId", nfcDataHolder.getScannedCardId());
            response.put("userId", nfcDataHolder.getScannedUserId());
        }
        
        return ResponseEntity.ok(response);
    }

    //書き込み実行 (認証ボタン押下
    @PostMapping("/write")
    public ResponseEntity<String> writeCard(@RequestBody WriterDto writerDto) {
        try {
            if (writerDto.getUserId() == null) {
                return ResponseEntity.badRequest().body("User ID is required");
            }
            
            // 書き込みサービス実行
        String result = writerService.issueCard(writerDto.getUserId(), writerDto.getCardId());
            
            // 成功したら状態をリセット(待機画面へ戻る)
            nfcDataHolder.reset();
            
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}