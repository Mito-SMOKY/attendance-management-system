package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID") 
    private Integer notificationId;

    @Column(name = "ReceiverUserID", nullable = false)
    private Integer receiverUserId; 

    @Column(name = "SenderUserID", nullable = false)
    private Integer senderUserId;

    @Column(name = "NotificationTypeID", nullable = false) 
    private Integer notificationTypeId;

    @Column(name = "Title")
    private String title;

    @Column(name = "Message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "CreatedAt", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "IsRead", nullable = false)
    private boolean isRead = false;

    @Column(name = "IsBookmarked", nullable = false)
    private boolean isBookmarked = false;

    // --- コンストラクタ ---
    public NotificationEntity() {}

    // --- Getter / Setter ---

    public Integer getNotificationId() { 
        return notificationId; 
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId; 
    }

    public Integer getReceiverUserId() { 
        return receiverUserId; 
    }

    public void setReceiverUserId(Integer receiverUserId) { 
        this.receiverUserId = receiverUserId; 
    }
    
    public Integer getSenderUserId() { 
        return senderUserId; 
    }

    public void setSenderUserId(Integer senderUserId) { 
        this.senderUserId = senderUserId; 
    }

    public Integer getNotificationTypeId() { 
        return notificationTypeId; 
    }

    public void setNotificationTypeId(Integer notificationTypeId) { 
        this.notificationTypeId = notificationTypeId; 
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() { 
        return message; 
    }

    public void setMessage(String message) { 
        this.message = message; 
    }

    public LocalDateTime getCreatedAt() { 
        return createdAt; 
    }

    public void setCreatedAt(LocalDateTime createdAt) { 
        this.createdAt = createdAt; 
    }

    public boolean isRead() { 
        return isRead; 
    }

    public void setRead(boolean isRead) { 
        this.isRead = isRead; 
    }

    public boolean isBookmarked() { 
        return isBookmarked; 
    }

    public void setBookmarked(boolean isBookmarked) { 
        this.isBookmarked = isBookmarked; 
    }
}