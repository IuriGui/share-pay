package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentUser;

class UserServiceTest {

    private final UserService userService = new UserService();

    @Test
    void createPaymentSplitsAmountEquallyBetweenUsers() {
        Payment payment = userService.createPayment(
            new BigDecimal("100.00"),
            List.of(BigInteger.ONE, BigInteger.TWO)
        );

        assertThat(payment.getAmount()).isEqualByComparingTo("100.00");
        assertThat(payment.getPayments()).hasSize(2);
        assertThat(payment.getPayments())
            .extracting(PaymentUser::getAmount)
            .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("50.00"));
    }

    @Test
    void createPaymentCreatesOneDistinctEntryPerUser() {
        List<BigInteger> userIds = List.of(BigInteger.ONE, BigInteger.TWO, BigInteger.TEN);

        Payment payment = userService.createPayment(new BigDecimal("90.00"), userIds);

        assertThat(payment.getPayments())
            .extracting(PaymentUser::getUser_id)
            .containsExactlyElementsOf(userIds);
        assertThat(payment.getPayments()).doesNotHaveDuplicates();
    }

    @Test
    void createPaymentDistributesRemainderCentsWithoutLosingMoney() {
        Payment payment = userService.createPayment(
            new BigDecimal("100.00"),
            List.of(BigInteger.ONE, BigInteger.TWO, BigInteger.TEN)
        );

        List<BigDecimal> shares = payment.getPayments().stream()
            .map(PaymentUser::getAmount)
            .toList();

        assertThat(shares.get(0)).isEqualByComparingTo("33.34");
        assertThat(shares.get(1)).isEqualByComparingTo("33.33");
        assertThat(shares.get(2)).isEqualByComparingTo("33.33");
        assertThat(shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add))
            .isEqualByComparingTo("100.00");
    }
}
