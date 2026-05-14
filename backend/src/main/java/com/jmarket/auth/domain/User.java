package com.jmarket.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(length = 30)
    private String name;

    @Column(length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean banned = false;

    @Column
    private LocalDateTime bannedUntil;

    @Column(length = 500)
    private String banReason;

    protected User() {
    }

    public User(String email, String passwordHash, String nickname) {
        this(email, passwordHash, nickname, UserRole.USER);
    }

    public User(String email, String passwordHash, String nickname, UserRole role) {
        this(email, passwordHash, nickname, null, null, role);
    }

    public User(
            String email,
            String passwordHash,
            String nickname,
            String name,
            String phoneNumber,
            UserRole role
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public UserRole getRole() {
        return role;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isBanned() {
        return banned && (bannedUntil == null || bannedUntil.isAfter(LocalDateTime.now()));
    }

    public LocalDateTime getBannedUntil() {
        return bannedUntil;
    }

    public String getBanReason() {
        return banReason;
    }

    public void ban(LocalDateTime bannedUntil, String banReason) {
        this.banned = true;
        this.bannedUntil = bannedUntil;
        this.banReason = banReason;
    }

    public void unban() {
        this.banned = false;
        this.bannedUntil = null;
        this.banReason = null;
    }
}
