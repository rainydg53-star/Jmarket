package com.jmarket.support.dto;

import com.jmarket.support.domain.SupportMajorCategory;
import com.jmarket.support.domain.SupportMinorCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupportInquiryCreateRequest(
        @NotNull SupportMajorCategory majorCategory,
        @NotNull SupportMinorCategory minorCategory,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String content
) {
}
