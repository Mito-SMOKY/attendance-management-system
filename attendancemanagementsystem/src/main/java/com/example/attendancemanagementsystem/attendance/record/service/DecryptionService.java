package com.example.attendancemanagementsystem.attendance.record.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

@Service
public class DecryptionService {

    private final byte[] xorKey;

    /**
     * コンストラクタ。application.properties から復号キーを読み込む。
     * @param xorKeyString application.propertiesで設定されたXORキー
     */
    public DecryptionService(@Value("${nfc.security.xor-key}") String xorKeyString) {
        this.xorKey = xorKeyString.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 暗号化された16進文字列のユーザーIDを復号し、オリジナルのユーザーID（文字列）を返す。
     * @param encryptedHex 暗号化されたユーザーIDの16進文字列
     * @return 復号されたユーザーID（文字列）
     */
    public String decryptUserId(String encryptedHex) {
        if (encryptedHex == null || encryptedHex.isEmpty()) {
            throw new IllegalArgumentException("Encrypted User ID is null or empty.");
        }
        
        // 1. 16進文字列をバイト配列に変換
        byte[] encryptedData = hexStringToByteArray(encryptedHex);
        byte[] decryptedData = new byte[encryptedData.length];
        
        // 2. XOR復号処理
        for (int i = 0; i < encryptedData.length; i++) {
            decryptedData[i] = (byte) (encryptedData[i] ^ xorKey[i % xorKey.length]);
        }
        
        // 3. バイト配列をUTF-8文字列に変換し、前後の空白を削除
        return new String(decryptedData, StandardCharsets.UTF_8).trim();
    }

    /**
     * 16進文字列をバイト配列に変換するユーティリティメソッド。
     * @param hex 16進文字列
     * @return 対応するバイト配列
     */
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