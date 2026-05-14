package com.jmarket.auth.service;

import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    public static final String COOKIE_NAME = "refreshToken";

    private static final String KEY_PREFIX = "jmarket:auth:refresh:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final Duration tokenTtl;

    public RefreshTokenService(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-token-validity-seconds:1209600}") long refreshTokenValiditySeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.tokenTtl = Duration.ofSeconds(refreshTokenValiditySeconds);
    }

    public String issue(Long userId) {
        byte[] randomBytes = new byte[48];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        redisTemplate.opsForValue().set(redisKey(token), String.valueOf(userId), tokenTtl);
        return token;
    }

    public Long rotate(String refreshToken) {
        Long userId = resolveUserId(refreshToken);
        revoke(refreshToken);
        return userId;
    }

    public Long resolveUserId(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new JmarketException(ErrorCode.UNAUTHORIZED);
        }
        String userId = redisTemplate.opsForValue().get(redisKey(refreshToken));
        if (userId == null || userId.isBlank()) {
            throw new JmarketException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException ex) {
            revoke(refreshToken);
            throw new JmarketException(ErrorCode.UNAUTHORIZED);
        }
    }

    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        redisTemplate.delete(redisKey(refreshToken));
    }

    public Duration tokenTtl() {
        return tokenTtl;
    }

    private String redisKey(String refreshToken) {
        return KEY_PREFIX + sha256(refreshToken);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
