package com.jmarket.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImageRequest(
        @NotBlank @Size(max = 1000) String imageUrl,
        boolean thumbnail
) {
}
