package com.jmarket.auth.service;

import com.jmarket.auth.dto.EmailVerificationConfirmRequest;
import com.jmarket.auth.dto.EmailVerificationConfirmResponse;
import com.jmarket.auth.dto.EmailVerificationSendRequest;
import com.jmarket.auth.dto.EmailVerificationSendResponse;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(20);

    private final Map<String, VerificationCode> codes = new ConcurrentHashMap<>();
    private final Map<String, VerifiedToken> verifiedTokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final Clock clock;
    private final String fromAddress;
    private final boolean exposeDevCode;
    private final boolean mailEnabled;

    @Autowired
    public EmailVerificationService(
            UserRepository userRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.auth.email-verification.from:no-reply@jmarket.local}") String fromAddress,
            @Value("${app.auth.email-verification.expose-dev-code:true}") boolean exposeDevCode,
            @Value("${app.auth.email-verification.mail-enabled:false}") boolean mailEnabled
    ) {
        this.userRepository = userRepository;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.clock = Clock.systemUTC();
        this.fromAddress = fromAddress;
        this.exposeDevCode = exposeDevCode;
        this.mailEnabled = mailEnabled;
    }

    public EmailVerificationSendResponse sendCode(EmailVerificationSendRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new JmarketException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        return issueCode(email, "회원가입 이메일 인증 코드");
    }

    public EmailVerificationSendResponse sendPasswordResetCode(EmailVerificationSendRequest request) {
        String email = normalizeEmail(request.email());
        if (!userRepository.existsByEmail(email)) {
            throw new JmarketException(ErrorCode.USER_NOT_FOUND);
        }
        return issueCode(email, "비밀번호 재설정 이메일 인증 코드");
    }

    public EmailVerificationConfirmResponse confirmCode(EmailVerificationConfirmRequest request) {
        String email = normalizeEmail(request.email());
        VerificationCode saved = codes.get(email);
        if (saved == null || saved.expiresAt().isBefore(clock.instant()) || !saved.code().equals(request.code())) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "이메일 인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        String token = UUID.randomUUID().toString();
        verifiedTokens.put(email, new VerifiedToken(token, clock.instant().plus(TOKEN_TTL)));
        codes.remove(email);
        return new EmailVerificationConfirmResponse(token);
    }

    public void validateVerifiedToken(String emailRaw, String token) {
        String email = normalizeEmail(emailRaw);
        VerifiedToken verifiedToken = verifiedTokens.get(email);
        if (token == null || token.isBlank()
                || verifiedToken == null
                || verifiedToken.expiresAt().isBefore(clock.instant())
                || !verifiedToken.token().equals(token)) {
            throw new JmarketException(ErrorCode.INVALID_INPUT, "이메일 인증을 완료해주세요.");
        }
        verifiedTokens.remove(email);
    }

    private EmailVerificationSendResponse issueCode(String email, String subjectSuffix) {
        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        Instant expiresAt = clock.instant().plus(CODE_TTL);
        codes.put(email, new VerificationCode(code, expiresAt));
        sendMail(email, code, subjectSuffix);
        return new EmailVerificationSendResponse(
                "인증 코드가 발송되었습니다.",
                Math.toIntExact(CODE_TTL.toSeconds()),
                exposeDevCode ? code : null
        );
    }

    private void sendMail(String email, String code, String subjectSuffix) {
        if (!mailEnabled || mailSender == null) {
            log.info("Email verification code for {} is {}", email, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject("[Jmarket] " + subjectSuffix);
            message.setText("Jmarket 인증 코드는 " + code + " 입니다. 5분 안에 입력해주세요.");
            mailSender.send(message);
        } catch (MailException error) {
            log.warn("Failed to send verification email to {}", email, error);
            throw new JmarketException(ErrorCode.INTERNAL_SERVER_ERROR, "인증 메일 발송에 실패했습니다.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private record VerificationCode(String code, Instant expiresAt) {
    }

    private record VerifiedToken(String token, Instant expiresAt) {
    }
}
