package com.jmarket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmarket.auth.security.JwtTokenProvider;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public RateLimitFilter(
            JwtTokenProvider jwtTokenProvider,
            ObjectMapper objectMapper,
            @Value("${jmarket.rate-limit.enabled:true}") boolean enabled
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitRule rule = resolveRule(request);
        if (!enabled || rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = rule.name() + ":" + resolveClientKey(request, rule.userScoped());
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rule.capacity())
                        .refillIntervally(rule.capacity(), rule.period())
                        .build())
                .build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        response.setStatus(ErrorCode.TOO_MANY_REQUESTS.httpStatus().value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(ErrorCode.TOO_MANY_REQUESTS));
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if ("POST".equals(method) && uri.matches("/api/auctions/\\d+/bids")) {
            return new RateLimitRule("auction-bid", 10, Duration.ofMinutes(1), true);
        }
        if ("POST".equals(method) && "/api/products/images".equals(uri)) {
            return new RateLimitRule("image-upload", 20, Duration.ofMinutes(1), true);
        }
        if ("POST".equals(method) && "/api/auth/login".equals(uri)) {
            return new RateLimitRule("login", 10, Duration.ofMinutes(1), false);
        }
        if ("POST".equals(method) && "/api/auth/signup".equals(uri)) {
            return new RateLimitRule("signup", 5, Duration.ofMinutes(1), false);
        }
        if ("POST".equals(method) && uri.startsWith("/api/auth/email-verification/")) {
            return new RateLimitRule("email-verification", 20, Duration.ofMinutes(1), false);
        }
        if (uri.startsWith("/api/auth/social/")) {
            return new RateLimitRule("social-auth", 10, Duration.ofMinutes(1), false);
        }
        return null;
    }

    private String resolveClientKey(HttpServletRequest request, boolean userScoped) {
        if (userScoped) {
            String token = resolveBearerToken(request);
            if (token != null && jwtTokenProvider.validate(token)) {
                try {
                    return "user:" + jwtTokenProvider.getUserId(token);
                } catch (RuntimeException ignored) {
                    // Fall through to IP-based limiting.
                }
            }
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private record RateLimitRule(
            String name,
            long capacity,
            Duration period,
            boolean userScoped
    ) {
    }
}
