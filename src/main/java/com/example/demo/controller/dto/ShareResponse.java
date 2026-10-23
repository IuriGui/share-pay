package com.example.demo.controller.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.demo.model.PaymentUser;

// parte de um participante vista pelo criador da cobrança, que é amigo de todos eles
public record ShareResponse(Long userId, String userName, BigDecimal amount, boolean paid, OffsetDateTime paidAt) {

    public static ShareResponse from(PaymentUser paymentUser) {
        return new ShareResponse(
            paymentUser.getUser().getId(),
            paymentUser.getUser().getName(),
            paymentUser.getAmount(),
            paymentUser.isPaid(),
            paymentUser.getPaidAt());
    }
}
