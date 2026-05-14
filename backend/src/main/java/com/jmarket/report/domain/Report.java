package com.jmarket.report.domain;

import com.jmarket.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(nullable = false, length = 5000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportResolutionAction resolutionAction;

    @Column(length = 1000)
    private String resolutionMemo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Report() {
    }

    public Report(User reporter, ReportTargetType targetType, Long targetId, String reason, String detail) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.detail = detail;
        this.status = ReportStatus.PENDING;
        this.resolutionAction = ReportResolutionAction.NONE;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void resolve(User admin, ReportStatus status, ReportResolutionAction action, String memo) {
        this.status = status;
        this.resolutionAction = action;
        this.resolutionMemo = memo;
        this.processedBy = admin;
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getReporter() {
        return reporter;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public ReportResolutionAction getResolutionAction() {
        return resolutionAction;
    }

    public String getResolutionMemo() {
        return resolutionMemo;
    }

    public User getProcessedBy() {
        return processedBy;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
