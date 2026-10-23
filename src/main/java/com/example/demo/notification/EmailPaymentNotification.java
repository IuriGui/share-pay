package com.example.demo.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentUser;
import com.example.demo.model.User;

// só é registrado quando há um servidor SMTP configurado (spring.mail.host)
@Component
@ConditionalOnProperty("spring.mail.host")
public class EmailPaymentNotification implements PaymentNotification {

    private final JavaMailSender mailSender;

    public EmailPaymentNotification(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void notifyPayment(PaymentUser paymentUser) {
        Payment payment = paymentUser.getPayment();
        User payer = paymentUser.getUser();
        User receiver = payment.getCreatedBy();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(receiver.getEmail());
        message.setSubject("Pagamento recebido: " + payment.getDescription());
        message.setText("%s pagou R$ %s referente a \"%s\"."
            .formatted(payer.getName(), paymentUser.getAmount(), payment.getDescription()));

        mailSender.send(message);
    }
}
