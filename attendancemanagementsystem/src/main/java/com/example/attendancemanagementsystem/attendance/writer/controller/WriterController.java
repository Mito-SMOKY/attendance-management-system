package com.example.attendancemanagementsystem.attendance.writer.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendancemanagementsystem.attendance.record.dto.RecordDTO;
import com.example.attendancemanagementsystem.attendance.record.service.DecryptionService;
import com.example.attendancemanagementsystem.attendance.writer.dto.WriterDto;
import com.example.attendancemanagementsystem.attendance.writer.service.WriterService;
import com.example.attendancemanagementsystem.common.component.NfcDataHolder;
import com.example.attendancemanagementsystem.common.entity.UsersEntity;
import com.example.attendancemanagementsystem.common.repository.UsersRepository;

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
            String displayLoginId = null;
            
            // 復号して検索
            if (recordDTO.getUserId() != null && !recordDTO.getUserId().replace("0", "").isEmpty()) {
                try {
                    // 暗号化データを復号
                    String decryptedUserIdStr = decryptionService.decryptUserId(recordDTO.getUserId());
                    
                    // 数値に変換
                    Integer userId = Integer.valueOf(decryptedUserIdStr);

                    // 学籍番号を取得
                    Optional<UsersEntity> userOpt = usersRepository.findById(userId);
                    
                    if (userOpt.isPresent()) {
                        displayLoginId = userOpt.get().getLoginId(); 
                        System.out.println("★ユーザー特定成功: " + displayLoginId);
                    }

                } catch (Exception e) {
                    System.out.println("★ID解決失敗(未登録カード等): " + e.getMessage());
                }
            }

            // 学籍番号セットして画面に渡す
            nfcDataHolder.setScannedData(
                recordDTO.getCardId(), 
                displayLoginId 
            );

            return ResponseEntity.ok("Scanned");

        } catch (Exception e) {
            nfcDataHolder.setScannedData(recordDTO.getCardId(), null); 
            return ResponseEntity.ok("Scanned (Error treated as New)");
        }
    }

    //ユーザー情報取得エンドポイント
    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo(@RequestParam("loginId") String loginId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 学籍番号などで検索
            Optional<UsersEntity> userOpt = usersRepository.findByLoginId(loginId);

            if (userOpt.isPresent()) {
                UsersEntity user = userOpt.get();
                response.put("status", "success");
                response.put("name", user.getName());
                response.put("internalUserId", user.getUserId());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "未登録のユーザーです");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
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
    
    // 書き込み実行 (認証ボタン押下)
    @PostMapping("/write")
    public ResponseEntity<String> writeCard(@RequestBody WriterDto writerDto) {
        try {
            // 学籍番号をチェック
            if (writerDto.getLoginId() == null || writerDto.getLoginId().isEmpty()) {
                return ResponseEntity.badRequest().body("Login ID (学籍番号) is required");
            }
            if (writerDto.getCardId() == null) {
                return ResponseEntity.badRequest().body("Card ID is required");
            }
            
            // 学籍番号からユーザーを検索して、学籍番号を取得する
            UsersEntity user = usersRepository.findByLoginId(writerDto.getLoginId())
                .orElseThrow(() -> new IllegalArgumentException("指定されたユーザー(学籍番号)が見つかりません"));
            
            // 書き込み処理
            String result = writerService.issueCard(user.getUserId(), writerDto.getCardId());
            
            nfcDataHolder.reset();
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace(); // サーバーログにエラー詳細を出す
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}