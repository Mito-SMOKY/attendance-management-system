package com.example.attendancemanagementsystem.attendance.record.service;
import com.example.attendancemanagementsystem.attendance.record.dto.RecordDTO; 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

@Service
public class RecordService {

    private final byte[] xorKey;

    // application.properties から復号キーを読み込む
    public RecordService(@Value("${nfc.security.xor-key}") String xorKeyString) {
        this.xorKey = xorKeyString.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * メイン処理 (Controllerから呼ばれる)
     */
    public void processAttendanceScan(RecordDTO recordDTO) { // (DTOのクラス名を使用)
        
        // DTOの 'getUserId()' メソッドを使って暗号化IDを取得
        String encryptedHex = recordDTO.getUserId(); 
        
        // 1. 復号処理
        String originalUserId = decryptUserId(encryptedHex);

        // 2. ログに出力 (DTOの各Getterを使用)
        System.out.println("==================================");
        System.out.println("NFCデータ 出欠記録 (復号成功)");
        System.out.println("  カードUID: " + recordDTO.getCardId()); 
        System.out.println("  リーダーID: " + recordDTO.getReaderId()); 
        System.out.println("  タイムスタンプ: " + recordDTO.getReadTime());
        System.out.println("  復号済みID: " + originalUserId); 
        System.out.println("==================================");
    }

    // --- 復号ロジック ---
    private String decryptUserId(String encryptedHex) {
        if (encryptedHex == null || encryptedHex.isEmpty()) {
            throw new IllegalArgumentException("Encrypted User ID is null or empty.");
        }
        byte[] encryptedData = hexStringToByteArray(encryptedHex);
        byte[] decryptedData = new byte[encryptedData.length];
        
        for (int i = 0; i < encryptedData.length; i++) {
            decryptedData[i] = (byte) (encryptedData[i] ^ xorKey[i % xorKey.length]);
        }
        return new String(decryptedData, StandardCharsets.UTF_8).trim();
    }

    // --- 16進文字列 -> byte[] 変換 ---
    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length.");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
