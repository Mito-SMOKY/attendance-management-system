package com.example.attendancemanagementsystem.common.entity;

import java.time.LocalDateTime;

import com.example.attendancemanagementsystem.common.enums.OtpPurpose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "otp")
public class OtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, unique = true)
    private UsersEntity user;

    @Column(name = "token")
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose")
    private OtpPurpose purpose;

    @Column(name = "expiredAt")
    private LocalDateTime expiredAt;

    @Column(name = "createAt")
    private LocalDateTime createAt;

    @Column(name = "attempts")
    private Integer attempts = 0;

    // --- コンストラクタ ---
    public OtpEntity() {}
    
    public OtpEntity(UsersEntity user) {
        this.user = user;
    }

    // --- Getter / Setter ---
    public Integer getId() { 
        return id; 
    }

    public void setId(Integer id) { 
        this.id = id; 
    } 

    public UsersEntity getUser() { 
        return user; 
    }

    public void setUser(UsersEntity user) { 
        this.user = user; 
    }

    public String getToken() { 
        return token; 
    }

    public void setToken(String token) { 
        this.token = token; 
    }

    public LocalDateTime getExpiredAt() { 
        return expiredAt; 
    }

    public OtpPurpose getPurpose() { 
        return purpose; 
    }

    public void setPurpose(OtpPurpose purpose) { 
        this.purpose = purpose; 
    }

    public void setExpiredAt(LocalDateTime expiredAt) { 
        this.expiredAt = expiredAt; 
    }

    public LocalDateTime getCreateAt() { 
        return createAt; 
    }

    public void setCreateAt(LocalDateTime createAt) { 
        this.createAt = createAt; 
    }

    public Integer getAttempts() { 
        return attempts; 
    }

    public void setAttempts(Integer attempts) { 
        this.attempts = attempts;
    }
}