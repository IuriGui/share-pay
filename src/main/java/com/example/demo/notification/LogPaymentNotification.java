package com.example.demo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.model.PaymentUser;

@Component
public class LogPaymentNotification implements PaymentNotification {

    private static final Logger log = LoggerFactory.getLogger(LogPaymentNotification.class);

    @Override
    public void notifyPayment(PaymentUser paymentUser) {
        log.info("Usuário {} pagou R$ {} da cobrança {}",
            paymentUser.getUser().getId(),
            paymentUser.getAmount(),
            paymentUser.getPayment().getId());
    }
}
