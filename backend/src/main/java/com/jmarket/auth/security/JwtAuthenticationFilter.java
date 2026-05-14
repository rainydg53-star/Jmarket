package com.jmarket.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SSE_STREAM_PATH = "/api/notifications/stream";
    private static final String SSE_ACCESS_TOKEN_PARAM = "accessToken";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            CustomUserDetailsService customUserDetailsService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && !token.isBlank()) {
            try {
                if (jwtTokenProvider.validate(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Long userId = jwtTokenProvider.getUserId(token);
                    CustomUserDetails userDetails =
                            (CustomUserDetails) customUserDetailsService.loadUserByUsername(
                                    jwtTokenProvider.parseClaims(token).get("email", String.class)
                            );

                    if (userDetails.getUser().getId().equals(userId)) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (RuntimeException ignored) {
                // Invalid/expired/orphaned token should not break the request pipeline.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        String requestUri = request.getRequestURI();
        if (requestUri != null && requestUri.endsWith(SSE_STREAM_PATH)) {
            return request.getParameter(SSE_ACCESS_TOKEN_PARAM);
        }
        return null;
    }
}
