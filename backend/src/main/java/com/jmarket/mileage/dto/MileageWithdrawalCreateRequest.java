package com.jmarket.mileage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MileageWithdrawalCreateRequest(
        @NotNull @Min(1000) Long amount,
        @NotBlank @Size(max = 40) String bankName,
        @NotBlank @Size(max = 40) String accountNumber,
        @NotBlank @Size(max = 40) String accountHolder
) {
}
