package com.example.demo.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentNotification;
import com.example.demo.model.PaymentUser;
import com.example.demo.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserService {

    private User user = new User(
        "John Doe",
        "john.doe@example.com",
        "password123",
        "04450213123"

    );

    // criar cobranca

    public Payment createPayment(BigDecimal amount, List<BigInteger> userIds) {
        // Dividir o total pelos usuários e criar uma cobrança para cada um

        Payment payment = new Payment(
            null, amount, "testando criação de cobranças", user, null
        );

        // divide() sem escala lança ArithmeticException em dízimas (ex.: 100 / 3),
        // então arredonda para centavos e distribui a sobra entre os primeiros usuários

        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("User IDs list cannot be null or empty");
            
        }

        BigDecimal individualAmount = amount.divide(BigDecimal.valueOf(userIds.size()), 2, RoundingMode.DOWN);
        BigDecimal remainder = amount.subtract(individualAmount.multiply(BigDecimal.valueOf(userIds.size())));
        BigDecimal cent = new BigDecimal("0.01");

        List<PaymentUser> paymentUsersList = new ArrayList<>();

        for (BigInteger userId : userIds) {
            // um objeto novo por iteração; reaproveitar a mesma instância fazia todas
            // as posições da lista apontarem para o mesmo PaymentUser
            PaymentUser paymentUser = new PaymentUser();
            paymentUser.setUser_id(userId);
            paymentUser.setPayment_id(payment.getId());

            BigDecimal share = individualAmount;
            if (remainder.compareTo(BigDecimal.ZERO) > 0) {
                share = share.add(cent);
                remainder = remainder.subtract(cent);
            }
            paymentUser.setAmount(share);
            paymentUsersList.add(paymentUser);
        }

        payment.setPayments(paymentUsersList);

        return payment;
    }



    //Pagar

    public void pay(PaymentUser paymentUserToPay) {
        // Implement payment logic here

        // pegar valor

        //Usar alguma api gratuita ou só exibir o pix na tela

        //Aguardar comprovante...

        //Validar comprovante

        //Marcar como paga

        //Notificar pagamento por... email? usadno a interface
    
    }   


    //Ver quem pagou

    public void checkWhoPaid() {
        // Implement logic to check who has paid
    }

}
