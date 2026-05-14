package com.jmarket.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 20000) String description,
        @Size(max = 40) String category,
        @NotNull @Min(0) Long price,
        @Size(max = 10) List<@Valid ProductImageRequest> images
) {
}
