package com.jmarket.auth.controller;

import com.jmarket.auth.dto.AccessTokenResponse;
import com.jmarket.auth.dto.AuthResponse;
import com.jmarket.auth.dto.ChangePasswordRequest;
import com.jmarket.auth.dto.EmailVerificationConfirmRequest;
import com.jmarket.auth.dto.EmailVerificationConfirmResponse;
import com.jmarket.auth.dto.EmailVerificationSendRequest;
import com.jmarket.auth.dto.EmailVerificationSendResponse;
import com.jmarket.auth.dto.FindIdRequest;
import com.jmarket.auth.dto.FindIdResponse;
import com.jmarket.auth.dto.LoginRequest;
import com.jmarket.auth.dto.PasswordResetRequest;
import com.jmarket.auth.dto.PasswordVerifyRequest;
import com.jmarket.auth.dto.PasswordVerifyResponse;
import com.jmarket.auth.dto.SignUpRequest;
import com.jmarket.auth.dto.SocialAuthorizeUrlResponse;
import com.jmarket.auth.dto.SocialLoginRequest;
import com.jmarket.auth.dto.UpdateProfileRequest;
import com.jmarket.auth.dto.UserMeResponse;
import com.jmarket.auth.service.AuthService;
import com.jmarket.auth.service.EmailVerificationService;
import com.jmarket.auth.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final boolean refreshTokenSecure;

    public AuthController(
            AuthService authService,
            EmailVerificationService emailVerificationService,
            @Value("${jwt.refresh-token-cookie-secure:false}") boolean refreshTokenSecure
    ) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenSecure = refreshTokenSecure;
    }

    @PostMapping("/signup")
    public AuthResponse signUp(@Valid @RequestBody SignUpRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.signUp(request);
        addRefreshCookie(response, authService.issueRefreshToken(authResponse.user().id()));
        return authResponse;
    }

    @PostMapping("/email-verification/send")
    public EmailVerificationSendResponse sendEmailVerificationCode(
            @Valid @RequestBody EmailVerificationSendRequest request
    ) {
        return emailVerificationService.sendCode(request);
    }

    @PostMapping("/email-verification/confirm")
    public EmailVerificationConfirmResponse confirmEmailVerificationCode(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        return emailVerificationService.confirmCode(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        addRefreshCookie(response, authService.issueRefreshToken(authResponse.user().id()));
        return authResponse;
    }

    @PostMapping("/find-id")
    public FindIdResponse findId(@Valid @RequestBody FindIdRequest request) {
        return authService.findLoginIdsByName(request);
    }

    @PostMapping("/password/verify-email")
    public EmailVerificationSendResponse verifyPasswordResetEmail(@Valid @RequestBody EmailVerificationSendRequest request) {
        return emailVerificationService.sendPasswordResetCode(request);
    }

    @PostMapping("/password/confirm-email")
    public EmailVerificationConfirmResponse confirmPasswordResetEmail(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        return emailVerificationService.confirmCode(request);
    }

    @PostMapping("/password/reset")
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
    }

    @GetMapping("/social/{provider}/authorize-url")
    public SocialAuthorizeUrlResponse socialAuthorizeUrl(
            @PathVariable String provider,
            @RequestParam(required = false) String state
    ) {
        return authService.getSocialAuthorizeUrl(provider, state);
    }

    @PostMapping("/social/{provider}/callback")
    public AuthResponse socialCallback(
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.socialLogin(provider, request);
        addRefreshCookie(response, authService.issueRefreshToken(authResponse.user().id()));
        return authResponse;
    }

    @PostMapping("/refresh")
    public AccessTokenResponse refresh(
            @CookieValue(name = RefreshTokenService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        AuthService.RefreshSession session = authService.refreshSession(refreshToken);
        addRefreshCookie(response, session.refreshToken());
        return session.response();
    }

    @PostMapping("/logout")
    public void logout(
            @CookieValue(name = RefreshTokenService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.revokeRefreshToken(refreshToken);
        clearRefreshCookie(response);
    }

    @GetMapping("/me")
    public UserMeResponse me(@AuthenticationPrincipal(expression = "username") String email) {
        return authService.getCurrentUser(email);
    }

    @PatchMapping("/me")
    public UserMeResponse updateMe(
            @AuthenticationPrincipal(expression = "username") String email,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return authService.updateCurrentUser(email, request);
    }

    @PatchMapping("/me/password")
    public void changeMyPassword(
            @AuthenticationPrincipal(expression = "username") String email,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changeCurrentUserPassword(email, request);
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = baseRefreshCookie(refreshToken)
                .maxAge(Duration.ofSeconds(authService.refreshTokenMaxAgeSeconds()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = baseRefreshCookie("")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseRefreshCookie(String value) {
        return ResponseCookie.from(RefreshTokenService.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(refreshTokenSecure)
                .sameSite("Lax")
                .path("/api/auth");
    }
}
