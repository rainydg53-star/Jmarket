package com.jmarket.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportInquiryAnswerRequest(
        @NotBlank @Size(max = 5000) String answerContent
) {
}
