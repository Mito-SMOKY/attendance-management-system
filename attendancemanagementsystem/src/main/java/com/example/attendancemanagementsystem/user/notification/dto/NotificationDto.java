package com.example.attendancemanagementsystem.user.notification.dto;

import java.time.LocalDateTime;

public class NotificationDto {
    private Integer notificationId;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private boolean read;
    private boolean bookmarked;

    public NotificationDto(Integer id, String title, String msg, LocalDateTime date, boolean read, boolean bookmarked) {
        this.notificationId = id;
        this.title = title;
        this.message = msg;
        this.createdAt = date;
        this.read = read;
        this.bookmarked = bookmarked;
    }

    // --- Getter / Setter ---

    public Integer getNotificationId() { 
        return notificationId; 
    }

    public void setNotificationId(Integer notificationId) { 
        this.notificationId = notificationId; 
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
        return read; 
    }

    public void setRead(boolean read) { 
        this.read = read; 
    }

    public boolean isBookmarked() { 
        return bookmarked; 
    }
    public void setBookmarked(boolean bookmarked) { 
        this.bookmarked = bookmarked; 
    }
}