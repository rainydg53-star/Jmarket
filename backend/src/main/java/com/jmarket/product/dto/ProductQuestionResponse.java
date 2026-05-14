package com.jmarket.product.dto;

import com.jmarket.auth.domain.User;
import com.jmarket.product.domain.ProductQuestion;
import java.time.LocalDateTime;

public record ProductQuestionResponse(
        Long id,
        Long questionerId,
        String questionerNickname,
        String question,
        boolean secret,
        boolean visible,
        String answer,
        LocalDateTime answeredAt,
        LocalDateTime createdAt
) {
    public static ProductQuestionResponse from(ProductQuestion productQuestion, User currentUser) {
        boolean visible = productQuestion.canView(currentUser);
        return new ProductQuestionResponse(
                productQuestion.getId(),
                productQuestion.getQuestioner().getId(),
                productQuestion.getQuestioner().getNickname(),
                visible ? productQuestion.getQuestion() : "비밀글입니다.",
                productQuestion.isSecret(),
                visible,
                visible ? productQuestion.getAnswer() : null,
                visible ? productQuestion.getAnsweredAt() : null,
                productQuestion.getCreatedAt()
        );
    }
}
