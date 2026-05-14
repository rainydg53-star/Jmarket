package com.jmarket.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-seconds}") long accessTokenValiditySeconds
    ) {
        this.secretKey = createSecretKey(secret);
        this.accessTokenValidityMs = accessTokenValiditySeconds * 1000;
    }

    public String generateAccessToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .subject(userDetails.getUser().getId().toString())
                .claim("email", userDetails.getUser().getEmail())
                .claim("role", userDetails.getUser().getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    private SecretKey createSecretKey(String secret) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException ignored) {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            Key key = Keys.hmacShaKeyFor(keyBytes);
            return (SecretKey) key;
        }
    }
}
