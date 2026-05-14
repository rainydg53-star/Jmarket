package com.jmarket.admin.repository;

import com.jmarket.admin.domain.AdminAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    List<AdminAuditLog> findTop30ByOrderByCreatedAtDesc();
}
