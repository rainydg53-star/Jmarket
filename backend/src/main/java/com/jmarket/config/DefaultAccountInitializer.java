package com.jmarket.config;

import com.jmarket.auth.domain.User;
import com.jmarket.auth.domain.UserRole;
import com.jmarket.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultAccountInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-id:1}")
    private String adminId;

    @Value("${app.seed.admin-password:1}")
    private String adminPassword;

    @Value("${app.seed.admin-nickname:슈퍼관리자}")
    private String adminNickname;

    @Value("${app.seed.user-id:2}")
    private String userId;

    @Value("${app.seed.user-password:2}")
    private String userPassword;

    @Value("${app.seed.user-nickname:관리자}")
    private String userNickname;

    @Value("${app.seed.user3-id:3}")
    private String user3Id;

    @Value("${app.seed.user3-password:3}")
    private String user3Password;

    @Value("${app.seed.user3-nickname:회원3}")
    private String user3Nickname;

    @Value("${app.seed.user4-id:4}")
    private String user4Id;

    @Value("${app.seed.user4-password:4}")
    private String user4Password;

    @Value("${app.seed.user4-nickname:회원4}")
    private String user4Nickname;

    @Value("${app.seed.user5-id:5}")
    private String user5Id;

    @Value("${app.seed.user5-password:5}")
    private String user5Password;

    @Value("${app.seed.user5-nickname:회원5}")
    private String user5Nickname;

    public DefaultAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureUser(adminId, adminPassword, adminNickname, UserRole.SUPER_ADMIN);
        ensureUser(userId, userPassword, userNickname, UserRole.ADMIN);
        ensureUser(user3Id, user3Password, user3Nickname, UserRole.USER);
        ensureUser(user4Id, user4Password, user4Nickname, UserRole.USER);
        ensureUser(user5Id, user5Password, user5Nickname, UserRole.USER);
    }

    private void ensureUser(String id, String rawPassword, String nickname, UserRole role) {
        userRepository.findByEmail(id).ifPresentOrElse(existing -> {
            if (!passwordEncoder.matches(rawPassword, existing.getPasswordHash())) {
                existing.changePasswordHash(passwordEncoder.encode(rawPassword));
                log.info("Updated password for seeded account id={}", id);
            }
            if (existing.getRole() != role) {
                existing.changeRole(role);
                log.info("Updated role for seeded account id={} role={}", id, role);
            }
            if (existing.getName() == null || !existing.getName().equals(existing.getNickname())) {
                existing.changeName(existing.getNickname());
                log.info("Updated name for seeded account id={} name={}", id, existing.getNickname());
            }
        }, () -> {
            String nicknameToUse = nickname;
            if (userRepository.existsByNickname(nicknameToUse)) {
                nicknameToUse = nickname + "_" + id;
            }
            User user = new User(id, passwordEncoder.encode(rawPassword), nicknameToUse, nicknameToUse, null, role);
            userRepository.save(user);
            log.info("Created seeded account id={} role={} nickname={} name={}", id, role, nicknameToUse, nicknameToUse);
        });
    }
}
