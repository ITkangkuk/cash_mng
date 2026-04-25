package com.example.mng.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String loginId;

    private String password;

    private String userName;

    private String phone;

    private String kakaoTargetYn;

    @Column(length = 50)
    private String sharedGroupCode;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhone() {
        return phone;
    }

    public String getKakaoTargetYn() {
        return kakaoTargetYn;
    }

    public String getSharedGroupCode() {
        return sharedGroupCode;
    }

    private String autoLoginToken;
    private LocalDateTime autoLoginExpireAt;

    public void setAutoLoginToken(String autoLoginToken) {
        this.autoLoginToken = autoLoginToken;
    }

    public void setAutoLoginExpireAt(LocalDateTime autoLoginExpireAt) {
        this.autoLoginExpireAt = autoLoginExpireAt;
    }

    public String getAutoLoginToken() {
        return autoLoginToken;
    }

    public LocalDateTime getAutoLoginExpireAt() {
        return autoLoginExpireAt;
    }
}
