package com.example.attendancemanagementsystem.common.utils;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TotpUtil {

    // 生成する桁数
    private static final int DIGITS = 6;
    
    // TOTPの時間ステップ（秒）
    private static final long TIME_STEP_SECONDS = 300; 

    /**
     * 現在時刻とシークレットキーを元にTOTPコードを生成します。
     * @param secretKey ユーザーごとの秘密鍵（またはランダムな文字列）
     * @return 6桁の数字文字列
     */
    public static String generateTotp(String secretKey) {
        long currentTimeSeconds = Instant.now().getEpochSecond();
        long counter = currentTimeSeconds / TIME_STEP_SECONDS;
        return generateTOTP(secretKey, counter);
    }

    // RFC 6238に基づいたTOTP生成ロジック
    private static String generateTOTP(String key, long counter) {
        try {
            // カウンターを8バイトのバイト列に変換
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            
            // HMAC-SHA1 アルゴリズムを使用
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec signKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);

            // ハッシュの最後のバイトの下位4ビットを取り出し、オフセットとする
            int offset = hash[hash.length - 1] & 0xF;

            // オフセットから4バイトを切り出して整数化
            long binary =
                    ((hash[offset] & 0x7f) << 24) |
                    ((hash[offset + 1] & 0xff) << 16) |
                    ((hash[offset + 2] & 0xff) << 8) |
                    (hash[offset + 3] & 0xff);

            // 10の6乗で割った余りを取得（6桁にする）
            long otp = binary % (long) Math.pow(10, DIGITS);

            // 前ゼロ埋めして文字列化（例: 123 -> "000123"）
            return String.format("%0" + DIGITS + "d", otp);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("TOTP生成エラー", e);
        }
    }
}