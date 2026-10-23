package com.example.demo.controller.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePaymentRequest(
    // NUMERIC(12,2) no banco
    @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
    @Size(max = 255) String description,
    @NotEmpty @Size(max = 50) List<@NotNull Long> userIds
) {
}
