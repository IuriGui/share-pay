package com.example.demo.controller.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.example.demo.model.Payment;

public record PaymentResponse(
    Long id,
    BigDecimal amount,
    String description,
    Long createdBy,
    OffsetDateTime createdAt,
    List<ShareResponse> shares
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getAmount(),
            payment.getDescription(),
            payment.getCreatedBy().getId(),
            payment.getCreatedAt(),
            payment.getPayments().stream().map(ShareResponse::from).toList());
    }
}
