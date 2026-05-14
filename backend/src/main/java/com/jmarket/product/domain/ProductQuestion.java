package com.jmarket.product.domain;

import com.jmarket.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "product_questions")
public class ProductQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questioner_id", nullable = false)
    private User questioner;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(nullable = false)
    private boolean secret;

    @Column(length = 1000)
    private String answer;

    @Column
    private LocalDateTime answeredAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected ProductQuestion() {
    }

    public ProductQuestion(Product product, User questioner, String question, boolean secret) {
        this.product = product;
        this.questioner = questioner;
        this.question = question;
        this.secret = secret;
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

    public void answer(String answer) {
        this.answer = answer;
        this.answeredAt = LocalDateTime.now();
    }

    public boolean canView(User user) {
        return !secret
                || product.getSeller().getId().equals(user.getId())
                || questioner.getId().equals(user.getId());
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public User getQuestioner() {
        return questioner;
    }

    public String getQuestion() {
        return question;
    }

    public boolean isSecret() {
        return secret;
    }

    public String getAnswer() {
        return answer;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
