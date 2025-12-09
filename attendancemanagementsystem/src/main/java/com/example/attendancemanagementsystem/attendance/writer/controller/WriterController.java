package com.example.attendancemanagementsystem.attendance.writer.controller;

import com.example.attendancemanagementsystem.attendance.record.dto.RecordDTO;
import com.example.attendancemanagementsystem.attendance.record.service.DecryptionService;
import com.example.attendancemanagementsystem.attendance.writer.dto.WriterDto;
import com.example.attendancemanagementsystem.attendance.writer.service.WriterService;
import com.example.attendancemanagementsystem.common.component.NfcDataHolder;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/issue")
public class WriterController {

    private final WriterService writerService;
    private final NfcDataHolder nfcDataHolder;
    private final UsersRepository usersRepository;
    private final DecryptionService decryptionService;

    // コンストラクタ 
    public WriterController(WriterService writerService, 
                            NfcDataHolder nfcDataHolder, 
                            UsersRepository usersRepository,
                            DecryptionService decryptionService) {
        this.writerService = writerService;
        this.nfcDataHolder = nfcDataHolder;
        this.usersRepository = usersRepository;
        this.decryptionService = decryptionService;
    }

    //登録用スキャン受信エンドポイント
    @PostMapping("/scan")
    public ResponseEntity<String> scanForRegistration(@RequestBody RecordDTO recordDTO) {
        try {
            // 復号処理
            String encryptedHex = recordDTO.getUserId();
            String originalUserIdString = decryptionService.decryptUserId(encryptedHex);
            
            // 書き込み画面にデータをセット
            nfcDataHolder.setScannedData(
                recordDTO.getCardId(), 
                originalUserIdString
            );
            
            System.out.println("★登録用スキャン検知: Card=" + recordDTO.getCardId() + ", User=" + originalUserIdString);
            return ResponseEntity.ok("Scanned for Registration");

        } catch (Exception e) {
            // 復号失敗やカード読み取りエラー時は「新規カード」として扱う
            nfcDataHolder.setScannedData(recordDTO.getCardId(), null); 
            return ResponseEntity.ok("Scanned (New Card)");
        }
    }

    @GetMapping("/user-info")
    public ResponseEntity<Map<String, String>> getUserInfo(@RequestParam Integer userId) {
        Map<String, String> response = new HashMap<>();
        
        Optional<UsersEntity> userOpt = usersRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            response.put("status", "success");
            response.put("name", userOpt.get().getName());
        } else {
            response.put("status", "error");
            response.put("message", "未登録のIDです");
        }
        return ResponseEntity.ok(response);
    }

    //状態ポーリング取得エンドポイント
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
    
    //書き込み実行 (認証ボタン押下)
    @PostMapping("/write")
    public ResponseEntity<String> writeCard(@RequestBody WriterDto writerDto) {
        try {
            if (writerDto.getUserId() == null) {
                return ResponseEntity.badRequest().body("User ID is required");
            }
            
            String result = writerService.issueCard(writerDto.getUserId(), writerDto.getCardId());
            nfcDataHolder.reset();
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}