package com.jmarket.report.repository;

import com.jmarket.report.domain.Report;
import com.jmarket.report.domain.ReportTargetType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByReporterIdOrderByCreatedAtDesc(Long reporterId);

    List<Report> findAllByOrderByCreatedAtDesc();

    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);
}
