package com.example.attendancemanagementsystem.common.component;

import org.springframework.stereotype.Component;

@Component
public class NfcDataHolder {

    private String scannedCardId = null;
    private String scannedUserId = null;
    private String currentStatus = "IDLE"; 
    private String errorMessage = null;   
    private long timestamp = System.currentTimeMillis();

    // 成功時 (書き込み画面へ)
    public synchronized void setScannedData(String cardId, String userId) {
        this.scannedCardId = cardId;
        this.scannedUserId = userId;
        this.currentStatus = "SCANNED";
        this.errorMessage = null;
        this.timestamp = System.currentTimeMillis();
    }

    // 失敗時 (読み込み失敗画面へ)
    public synchronized void setError(String message) {
        this.currentStatus = "ERROR";
        this.errorMessage = message;
        this.timestamp = System.currentTimeMillis();
    }

    public synchronized void reset() {
        this.currentStatus = "IDLE";
        this.scannedCardId = null;
        this.scannedUserId = null;
        this.errorMessage = null;
        this.timestamp = System.currentTimeMillis();
    }

    // Getter
    public synchronized String getScannedCardId() {
        return scannedCardId; 
    }

    public synchronized String getScannedUserId() { 
        return scannedUserId; 
    }

    public synchronized String getCurrentStatus() {
        return currentStatus;
    }

    public synchronized String getErrorMessage() {
        return errorMessage; 
    }

    public synchronized long getTimestamp() { 
        return timestamp; 
    }
}