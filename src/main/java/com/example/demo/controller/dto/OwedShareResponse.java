package com.example.demo.controller.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentUser;
import com.example.demo.model.User;

// uma parte que o usuário deve: traz só a parte dele, nunca a dos outros participantes,
// e a chave Pix de quem criou a cobrança para que ele possa pagar
public record OwedShareResponse(
    Long paymentId,
    String description,
    BigDecimal total,
    OffsetDateTime createdAt,
    Long creatorId,
    String creatorName,
    String creatorPaymentKey,
    BigDecimal amount,
    boolean paid,
    OffsetDateTime paidAt
) {

    public static OwedShareResponse from(PaymentUser paymentUser) {
        Payment payment = paymentUser.getPayment();
        User creator = payment.getCreatedBy();
        return new OwedShareResponse(
            payment.getId(),
            payment.getDescription(),
            payment.getAmount(),
            payment.getCreatedAt(),
            creator.getId(),
            creator.getName(),
            creator.getPaymentKey(),
            paymentUser.getAmount(),
            paymentUser.isPaid(),
            paymentUser.getPaidAt());
    }
}
