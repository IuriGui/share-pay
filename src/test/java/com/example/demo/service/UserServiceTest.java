package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.model.Payment;
import com.example.demo.model.PaymentUser;
import com.example.demo.model.PaymentUserId;
import com.example.demo.model.User;
import com.example.demo.notification.PaymentNotification;
import com.example.demo.repository.FriendshipRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.PaymentUserRepository;
import com.example.demo.repository.UserRepository;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentUserRepository paymentUserRepository = mock(PaymentUserRepository.class);
    private final FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);
    private final PaymentNotification notification = mock(PaymentNotification.class);

    private final UserService userService = new UserService(
        userRepository, paymentRepository, paymentUserRepository, friendshipRepository, List.of(notification)
    );

    private final User creator = user(100L);

    @BeforeEach
    void setUp() {
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createPaymentSplitsAmountEquallyBetweenUsersAndCreator() {
        givenUsers(1L, 2L);

        // 3 partes: os 2 convidados e o criador, que não gera linha em payment_user
        Payment payment = userService.createPayment(creator.getId(), new BigDecimal("90.00"), "jantar", List.of(1L, 2L));

        assertThat(payment.getAmount()).isEqualByComparingTo("90.00");
        assertThat(payment.getCreatedBy()).isSameAs(creator);
        assertThat(payment.getPayments()).hasSize(2);
        assertThat(payment.getPayments())
            .extracting(PaymentUser::getAmount)
            .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("30.00"));
        verify(paymentRepository).save(payment);
    }

    @Test
    void createPaymentDoesNotChargeAnExtraCentWhenThereIsNoRemainder() {
        givenUsers(1L);

        Payment payment = userService.createPayment(creator.getId(), new BigDecimal("100.00"), "jantar", List.of(1L));

        assertThat(payment.getPayments().get(0).getAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void createPaymentCreatesOneDistinctEntryPerUser() {
        List<Long> userIds = List.of(1L, 2L, 10L);
        givenUsers(10L, 1L, 2L);

        Payment payment = userService.createPayment(creator.getId(), new BigDecimal("90.00"), "jantar", userIds);

        assertThat(payment.getPayments())
            .extracting(paymentUser -> paymentUser.getUser().getId())
            .containsExactlyElementsOf(userIds);
        assertThat(payment.getPayments()).doesNotHaveDuplicates();
        assertThat(payment.getPayments()).allSatisfy(paymentUser -> assertThat(paymentUser.getPayment()).isSameAs(payment));
    }

    @Test
    void createPaymentDistributesRemainderCentsWithoutLosingMoney() {
        givenUsers(1L, 2L, 10L);

        // 4 partes de 25.0025: sobram 1 centavo além das partes de 25.00, que vai para o primeiro
        Payment payment = userService.createPayment(creator.getId(), new BigDecimal("100.01"), "jantar", List.of(1L, 2L, 10L));

        List<BigDecimal> shares = payment.getPayments().stream()
            .map(PaymentUser::getAmount)
            .toList();

        assertThat(shares.get(0)).isEqualByComparingTo("25.01");
        assertThat(shares.get(1)).isEqualByComparingTo("25.00");
        assertThat(shares.get(2)).isEqualByComparingTo("25.00");
        BigDecimal creatorShare = new BigDecimal("100.01").subtract(shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        assertThat(creatorShare).isEqualByComparingTo("25.00");
    }

    @Test
    void createPaymentRejectsUsersWhoAreNotFriends() {
        givenUsers(1L);

        // o usuário 2 pode nem existir: a mensagem é a mesma nos dois casos
        assertThatThrownBy(() -> userService.createPayment(creator.getId(), new BigDecimal("10.00"), "jantar", List.of(1L, 2L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User 2 is not your friend");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPaymentRejectsChargingYourself() {
        givenUsers(1L);

        assertThatThrownBy(() -> userService.createPayment(creator.getId(), new BigDecimal("10.00"), "jantar", List.of(1L, creator.getId())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not your friend");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPaymentRejectsDuplicatedUsers() {
        givenUsers(1L);

        assertThatThrownBy(() -> userService.createPayment(creator.getId(), new BigDecimal("10.00"), "jantar", List.of(1L, 1L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("duplicates");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void payMarksAsPaidAndNotifies() {
        PaymentUser paymentUser = paymentUser(5L, 1L);

        PaymentUser paid = userService.pay(5L, 1L);

        assertThat(paid.isPaid()).isTrue();
        assertThat(paid.getPaidAt()).isNotNull();
        verify(paymentUserRepository).save(paymentUser);
        verify(notification).notifyPayment(paymentUser);
    }

    @Test
    void payTwiceFailsWithoutNotifyingAgain() {
        PaymentUser paymentUser = paymentUser(5L, 1L);
        paymentUser.setPaid(true);

        assertThatThrownBy(() -> userService.pay(5L, 1L)).isInstanceOf(IllegalStateException.class);
        verify(notification, never()).notifyPayment(any());
    }

    private PaymentUser paymentUser(Long paymentId, Long userId) {
        Payment payment = new Payment(new BigDecimal("10.00"), "jantar", creator);
        payment.setId(paymentId);
        PaymentUser paymentUser = new PaymentUser(user(userId), new BigDecimal("10.00"));
        payment.addPaymentUser(paymentUser);
        when(paymentUserRepository.findById(new PaymentUserId(paymentId, userId))).thenReturn(Optional.of(paymentUser));
        return paymentUser;
    }

    // os usuários informados existem e são amigos do criador
    private void givenUsers(Long... ids) {
        List<User> users = java.util.Arrays.stream(ids).map(UserServiceTest::user).toList();
        when(userRepository.findAllById(any())).thenReturn(users);
        when(friendshipRepository.findFriendIdsAmong(eq(creator.getId()), any())).thenReturn(List.of(ids));
    }

    private static User user(Long id) {
        User user = new User("user" + id, "user" + id + "@example.com", "secret", "pix-" + id);
        user.setId(id);
        return user;
    }
}
