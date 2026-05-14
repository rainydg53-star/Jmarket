package com.jmarket.support.domain;

import com.jmarket.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "support_inquiries")
public class SupportInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @Convert(converter = SupportMajorCategoryConverter.class)
    @Column(nullable = false, length = 30)
    private SupportMajorCategory majorCategory;

    @Convert(converter = SupportMinorCategoryConverter.class)
    @Column(nullable = false, length = 30)
    private SupportMinorCategory minorCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportInquiryStatus status;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(length = 5000)
    private String answerContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private User answeredBy;

    @Column
    private LocalDateTime answeredAt;

    @Column
    private LocalDateTime closedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected SupportInquiry() {
    }

    public SupportInquiry(
            User member,
            SupportMajorCategory majorCategory,
            SupportMinorCategory minorCategory,
            String title,
            String content
    ) {
        this.member = member;
        this.majorCategory = majorCategory;
        this.minorCategory = minorCategory;
        this.status = SupportInquiryStatus.WAITING;
        this.title = title;
        this.content = content;
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

    public void answer(User admin, String answerContent) {
        this.answeredBy = admin;
        this.answerContent = answerContent;
        this.answeredAt = LocalDateTime.now();
        this.status = SupportInquiryStatus.ANSWERED;
    }

    public void changeStatus(SupportInquiryStatus status) {
        this.status = status;
        if (status == SupportInquiryStatus.CLOSED) {
            this.closedAt = LocalDateTime.now();
            return;
        }
        this.closedAt = null;
    }

    public Long getId() {
        return id;
    }

    public User getMember() {
        return member;
    }

    public SupportMajorCategory getMajorCategory() {
        return majorCategory;
    }

    public SupportMinorCategory getMinorCategory() {
        return minorCategory;
    }

    public SupportInquiryStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAnswerContent() {
        return answerContent;
    }

    public User getAnsweredBy() {
        return answeredBy;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
