package com.jmarket.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductQuestionAnswerRequest(
        @NotBlank @Size(max = 1000) String answer
) {
}
