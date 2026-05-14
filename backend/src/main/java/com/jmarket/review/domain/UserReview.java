package com.jmarket.review.domain;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"reviewer_id", "source_type", "source_id"})
)
public class UserReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewSourceType sourceType;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected UserReview() {
    }

    public UserReview(User reviewer, User targetUser, ReviewSourceType sourceType, Long sourceId, int rating, String content) {
        this.reviewer = reviewer;
        this.targetUser = targetUser;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.rating = rating;
        this.content = content;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getReviewer() { return reviewer; }
    public User getTargetUser() { return targetUser; }
    public ReviewSourceType getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public int getRating() { return rating; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
