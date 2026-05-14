package com.jmarket.admin.service;

import com.jmarket.admin.domain.AdminAuditLog;
import com.jmarket.admin.dto.AdminAuditLogResponse;
import com.jmarket.admin.repository.AdminAuditLogRepository;
import com.jmarket.auth.domain.User;
import com.jmarket.auth.repository.UserRepository;
import com.jmarket.common.exception.ErrorCode;
import com.jmarket.common.exception.JmarketException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {

    private final AdminAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AdminAuditService(AdminAuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void log(String adminEmail, String action, String targetType, Long targetId, String memo) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new JmarketException(ErrorCode.USER_NOT_FOUND));
        auditLogRepository.save(new AdminAuditLog(admin, action, targetType, targetId, memo));
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLogResponse> getRecentLogs() {
        return auditLogRepository.findTop30ByOrderByCreatedAtDesc().stream()
                .map(AdminAuditLogResponse::from)
                .toList();
    }
}
