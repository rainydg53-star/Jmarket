package com.jmarket.auth.service;

import com.jmarket.admin.repository.UserRestrictionRepository;
import com.jmarket.auth.domain.User;
import com.jmarket.auth.domain.UserRole;
import com.jmarket.auth.dto.AuthResponse;
import com.jmarket.auth.dto.AccessTokenResponse;
import com.jmarket.auth.dto.ChangePasswordRequest;
import com.jmarket.auth.dto.LoginRequest;
import com.jmarket.auth.dto.FindIdRequest;
import com.jmarket.auth.dto.FindIdResponse;
import com.jmarket.auth.dto.PasswordResetRequest;
import com.jmarket.auth.dto.PasswordVerifyRequest;
import com.jmarket.auth.dto.PasswordVerifyResponse;
import com.jmarket.auth.dto.SignUpRequest;
import com.jmarket.auth.dto.SocialAuthorizeUrlResponse;
import com.jmarket.auth.dto.SocialLoginRequest;
import com.jmarket.auth.dto.UpdateProfileRequest;
import com.jmarket.auth.dto.UserMeResponse;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.auth.security.CustomUserDetails;
import com.jmarket.auth.security.JwtTokenProvider;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRestrictionRepository userRestrictionRepository;
    private final EmailVerificationService emailVerificationService;
    private final RestClient restClient;

    @Value("${social.kakao.client-id:}")
    private String kakaoClientId;
    @Value("${social.kakao.client-secret:}")
    private String kakaoClientSecret;
    @Value("${social.kakao.redirect-uri:http://localhost:5173/oauth/callback/kakao}")
    private String kakaoRedirectUri;
    @Value("${social.kakao.authorize-uri:https://kauth.kakao.com/oauth/authorize}")
    private String kakaoAuthorizeUri;
    @Value("${social.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String kakaoTokenUri;
    @Value("${social.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}")
    private String kakaoUserInfoUri;

    @Value("${social.naver.client-id:}")
    private String naverClientId;
    @Value("${social.naver.client-secret:}")
    private String naverClientSecret;
    @Value("${social.naver.redirect-uri:http://localhost:5173/oauth/callback/naver}")
    private String naverRedirectUri;
    @Value("${social.naver.authorize-uri:https://nid.naver.com/oauth2.0/authorize}")
    private String naverAuthorizeUri;
    @Value("${social.naver.token-uri:https://nid.naver.com/oauth2.0/token}")
    private String naverTokenUri;
    @Value("${social.naver.user-info-uri:https://openapi.naver.com/v1/nid/me}")
    private String naverUserInfoUri;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            UserRestrictionRepository userRestrictionRepository,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.userRestrictionRepository = userRestrictionRepository;
        this.emailVerificationService = emailVerificationService;
        this.restClient = RestClient.create();
    }

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new JmarketException(ErrorCode.INVALID_INPUT);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new JmarketException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new JmarketException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
        emailVerificationService.validateVerifiedToken(request.email(), request.emailVerificationToken());

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.name(),
                request.phoneNumber(),
                UserRole.USER
        );
        User savedUser = userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        return new AuthResponse(accessToken, UserMeResponse.from(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            userRepository.findByEmail(request.loginId())
                    .filter(User::isBanned)
                    .ifPresent(this::throwBannedUserException);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.loginId(), request.password())
            );
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
            return new AuthResponse(accessToken, UserMeResponse.from(userDetails.getUser()));
        } catch (JmarketException ex) {
            throw ex;
        } catch (BadCredentialsException ex) {
            throw new JmarketException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Transactional(readOnly = true)
    public SocialAuthorizeUrlResponse getSocialAuthorizeUrl(String providerRaw, String state) {
        SocialProvider provider = SocialProvider.from(providerRaw);
        String authorizeUrl = switch (provider) {
            case KAKAO -> buildKakaoAuthorizeUrl();
            case NAVER -> buildNaverAuthorizeUrl(state);
        };
        return new SocialAuthorizeUrlResponse(authorizeUrl);
    }

    @Transactional
    public AuthResponse socialLogin(String providerRaw, SocialLoginRequest request) {
        SocialProvider provider = SocialProvider.from(providerRaw);
        if (request.code() == null || request.code().isBlank()) {
            throw new JmarketException(ErrorCode.INVALID_INPUT);
        }

        SocialUserProfile socialUser = switch (provider) {
            case KAKAO -> resolveKakaoUser(request.code());
            case NAVER -> resolveNaverUser(request.code(), request.state());
        };

        User user = userRepository.findByEmail(socialUser.email())
                .map(existing -> updateSocialUserProfile(existing, socialUser))
                .orElseGet(() -> createSocialUser(socialUser));
        if (user.isBanned()) {
            throwBannedUserException(user);
        }
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        return new AuthResponse(accessToken, UserMeResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserMeResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
        return UserMeResponse.from(user, getActiveRestrictionTypes(user));
    }

    @Transactional
    public UserMeResponse updateCurrentUser(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));

        String nextNickname = request.nickname().trim();
        if (!user.getNickname().equals(nextNickname) && userRepository.existsByNickname(nextNickname)) {
            throw new JmarketException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        user.changeName(request.name().trim());
        user.changeNickname(nextNickname);
        user.changePhoneNumber(request.phoneNumber().trim());
        return UserMeResponse.from(user, getActiveRestrictionTypes(user));
    }

    @Transactional
    public void changeCurrentUserPassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new JmarketException(ErrorCode.INVALID_CREDENTIALS, "현재 비밀번호가 일치하지 않습니다.");
        }
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "새 비밀번호 확인이 일치하지 않습니다.");
        }

        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional(readOnly = true)
    public RefreshSession refreshSession(String refreshToken) {
        Long userId = refreshTokenService.rotate(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
        if (user.isBanned()) {
            throwBannedUserException(user);
        }
        String accessToken = jwtTokenProvider.generateAccessToken(new CustomUserDetails(user));
        String nextRefreshToken = refreshTokenService.issue(user.getId());
        return new RefreshSession(new AccessTokenResponse(accessToken), nextRefreshToken);
    }

    public String issueRefreshToken(Long userId) {
        return refreshTokenService.issue(userId);
    }

    public void revokeRefreshToken(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    public long refreshTokenMaxAgeSeconds() {
        return refreshTokenService.tokenTtl().toSeconds();
    }

    @Transactional(readOnly = true)
    public FindIdResponse findLoginIdsByName(FindIdRequest request) {
        List<String> ids = userRepository.findAllByName(request.name().trim()).stream()
                .map(User::getEmail)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            throw new JmarketException(ErrorCode.USER_NOT_FOUND);
        }
        return new FindIdResponse(ids);
    }

    @Transactional(readOnly = true)
    public PasswordVerifyResponse verifyPasswordResetEmail(PasswordVerifyRequest request) {
        userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
        return new PasswordVerifyResponse(true);
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "새 비밀번호 확인이 일치하지 않습니다.");
        }
        User user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
        emailVerificationService.validateVerifiedToken(request.email(), request.emailVerificationToken());
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private String buildKakaoAuthorizeUrl() {
        requireClientId(kakaoClientId);
        return UriComponentsBuilder.fromUriString(kakaoAuthorizeUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", kakaoClientId)
                .queryParam("redirect_uri", kakaoRedirectUri)
                .build()
                .toUriString();
    }

    private String buildNaverAuthorizeUrl(String state) {
        requireClientId(naverClientId);
        String safeState = (state == null || state.isBlank()) ? UUID.randomUUID().toString() : state;
        return UriComponentsBuilder.fromUriString(naverAuthorizeUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", naverClientId)
                .queryParam("redirect_uri", naverRedirectUri)
                .queryParam("state", safeState)
                .build()
                .toUriString();
    }

    private SocialUserProfile resolveKakaoUser(String code) {
        requireClientId(kakaoClientId);

        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("grant_type", "authorization_code");
        tokenRequest.add("client_id", kakaoClientId);
        tokenRequest.add("redirect_uri", kakaoRedirectUri);
        tokenRequest.add("code", code);
        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
            tokenRequest.add("client_secret", kakaoClientSecret);
        }

        Map<?, ?> tokenResponse = postForm(kakaoTokenUri, tokenRequest);
        String accessToken = asString(tokenResponse.get("access_token"));
        if (accessToken == null || accessToken.isBlank()) {
            throw new JmarketException(ErrorCode.SOCIAL_AUTH_FAILED);
        }

        Map<?, ?> userInfo = restClient.get()
                .uri(kakaoUserInfoUri)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);
        if (userInfo == null) {
            throw new JmarketException(ErrorCode.SOCIAL_AUTH_FAILED);
        }

        String providerUserId = asString(userInfo.get("id"));
        Map<?, ?> kakaoAccount = asMap(userInfo.get("kakao_account"));
        Map<?, ?> profile = asMap(kakaoAccount.get("profile"));

        validateProviderUserId(providerUserId);
        String email = buildSocialLoginEmail("kakao", providerUserId);
        String name = firstNotBlank(asString(profile.get("nickname")), "카카오사용자");
        String nicknameSeed = name;
        return new SocialUserProfile("kakao", providerUserId, email, nicknameSeed, name);
    }

    private SocialUserProfile resolveNaverUser(String code, String state) {
        requireClientId(naverClientId);
        if (naverClientSecret == null || naverClientSecret.isBlank()) {
            throw new JmarketException(ErrorCode.SOCIAL_AUTH_FAILED);
        }

        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("grant_type", "authorization_code");
        tokenRequest.add("client_id", naverClientId);
        tokenRequest.add("client_secret", naverClientSecret);
        tokenRequest.add("code", code);
        tokenRequest.add("state", state == null ? "" : state);

        Map<?, ?> tokenResponse = postForm(naverTokenUri, tokenRequest);
        String accessToken = asString(tokenResponse.get("access_token"));
        if (accessToken == null || accessToken.isBlank()) {
            throw new JmarketException(ErrorCode.SOCIAL_AUTH_FAILED);
        }

        Map<?, ?> userInfo = restClient.get()
                .uri(naverUserInfoUri)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);
        if (userInfo == null) {
            throw new JmarketException(ErrorCode.SOCIAL_AUTH_FAILED);
        }

        Map<?, ?> response = asMap(userInfo.get("response"));
        String providerUserId = asString(response.get("id"));
        validateProviderUserId(providerUserId);
        String email = buildSocialLoginEmail("naver", providerUserId);
        String name = firstNotBlank(asString(response.get("name")), asString(response.get("nickname")), "네이버사용자");
        String nicknameSeed = firstNotBlank(asString(response.get("nickname")), name);
        return new SocialUserProfile("naver", providerUserId, email, nicknameSeed, name);
    }

    private Map<?, ?> postForm(String uri, MultiValueMap<String, String> formData) {
        try {
            Map<?, ?> response = restClient.post()
                    .uri(uri)
                    .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                throw new JmarketException(ErrorCode.SOCIAL_AUTH_FAILED);
            }
            return response;
        } catch (RestClientResponseException ex) {
            log.warn(
                    "Social OAuth token request failed. uri={}, status={}, body={}",
                    uri,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );
            throw new JmarketException(ErrorCode.SOCIAL_AUTH_FAILED);
        } catch (Exception ex) {
            log.warn("Social OAuth token request failed. uri={}", uri, ex);
            throw new JmarketException(ErrorCode.SOCIAL_AUTH_FAILED);
        }
    }

    private User createSocialUser(SocialUserProfile socialUser) {
        String nickname = generateUniqueNickname(socialUser.provider(), socialUser.nicknameSeed(), socialUser.providerUserId());
        String passwordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        User user = new User(
                socialUser.email(),
                passwordHash,
                nickname,
                socialUser.name(),
                null,
                UserRole.USER
        );
        return userRepository.save(user);
    }

    private User updateSocialUserProfile(User user, SocialUserProfile socialUser) {
        String nextName = firstNotBlank(socialUser.name(), user.getName());
        if (nextName != null && !nextName.equals(user.getName())) {
            user.changeName(nextName);
        }

        if (isGeneratedSocialNickname(user.getNickname(), socialUser.provider())) {
            String nextNickname = generateUniqueNickname(
                    socialUser.provider(),
                    socialUser.nicknameSeed(),
                    socialUser.providerUserId(),
                    user.getNickname()
            );
            if (!nextNickname.equals(user.getNickname())) {
                user.changeNickname(nextNickname);
            }
        }
        return user;
    }

    private String generateUniqueNickname(String provider, String nicknameSeed, String providerUserId) {
        return generateUniqueNickname(provider, nicknameSeed, providerUserId, null);
    }

    private String generateUniqueNickname(String provider, String nicknameSeed, String providerUserId, String currentNickname) {
        String base = sanitizeNickname(nicknameSeed);
        if (base.length() < 2) {
            base = sanitizeNickname(provider + "_" + providerUserId);
        }
        if (base.length() < 2) {
            base = provider + "_user";
        }
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        if (base.equals(currentNickname)) {
            return base;
        }
        if (!userRepository.existsByNickname(base)) {
            return base;
        }
        for (int i = 1; i <= 999; i++) {
            String suffix = "_" + i;
            int maxBaseLength = Math.max(2, 20 - suffix.length());
            String candidate = base.length() > maxBaseLength ? base.substring(0, maxBaseLength) : base;
            candidate = candidate + suffix;
            if (candidate.equals(currentNickname)) {
                return candidate;
            }
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        return provider + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String sanitizeNickname(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[^a-zA-Z0-9\\uAC00-\\uD7A3]", "");
    }

    private boolean isGeneratedSocialNickname(String nickname, String provider) {
        return nickname == null
                || nickname.isBlank()
                || nickname.equals(provider + "_user")
                || nickname.startsWith(provider + "_");
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private void validateProviderUserId(String providerUserId) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new JmarketException(ErrorCode.SOCIAL_AUTH_FAILED);
        }
    }

    private String buildSocialLoginEmail(String provider, String providerUserId) {
        String normalizedId = providerUserId.replaceAll("[^a-zA-Z0-9]", "");
        if (normalizedId.isBlank()) {
            normalizedId = UUID.randomUUID().toString().replace("-", "");
        }
        return provider + "_" + normalizedId + "@social.jmarket.local";
    }

    private void requireClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new JmarketException(ErrorCode.SOCIAL_AUTH_FAILED);
        }
    }

    private void throwBannedUserException(User user) {
        StringBuilder message = new StringBuilder("정지된 계정입니다.");
        if (user.getBanReason() != null && !user.getBanReason().isBlank()) {
            message.append(" 사유: ").append(user.getBanReason());
        }
        if (user.getBannedUntil() != null) {
            message.append(" 해제 예정: ").append(user.getBannedUntil());
        }
        throw new JmarketException(ErrorCode.USER_BANNED, message.toString());
    }

    private List<String> getActiveRestrictionTypes(User user) {
        return userRestrictionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(restriction -> restriction.isCurrentlyActive())
                .map(restriction -> restriction.getType().name())
                .distinct()
                .toList();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return Map.of();
    }

    private enum SocialProvider {
        KAKAO, NAVER;

        static SocialProvider from(String value) {
            try {
                return SocialProvider.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (Exception ex) {
                throw new JmarketException(ErrorCode.INVALID_INPUT);
            }
        }
    }

    private record SocialUserProfile(
            String provider,
            String providerUserId,
            String email,
            String nicknameSeed,
            String name
    ) {
    }

    public record RefreshSession(
            AccessTokenResponse response,
            String refreshToken
    ) {
    }
}
