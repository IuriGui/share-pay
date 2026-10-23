package com.example.demo.controller.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.demo.model.PaymentUser;

public record PaymentShareResponse(Long paymentId, Long userId, BigDecimal amount, boolean paid, OffsetDateTime paidAt) {

    public static PaymentShareResponse from(PaymentUser paymentUser) {
        return new PaymentShareResponse(
            paymentUser.getPayment().getId(),
            paymentUser.getUser().getId(),
            paymentUser.getAmount(),
            paymentUser.isPaid(),
            paymentUser.getPaidAt());
    }
}
