package com.example.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.exception.ConflictException;
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentUser;
import com.example.demo.model.PaymentUserId;
import com.example.demo.model.User;
import com.example.demo.notification.PaymentNotification;
import com.example.demo.repository.FriendshipRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.PaymentUserRepository;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentUserRepository paymentUserRepository;
    private final FriendshipRepository friendshipRepository;
    private final List<PaymentNotification> notifications;

    public UserService(
        UserRepository userRepository,
        PaymentRepository paymentRepository,
        PaymentUserRepository paymentUserRepository,
        FriendshipRepository friendshipRepository,
        List<PaymentNotification> notifications
    ) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.paymentUserRepository = paymentUserRepository;
        this.friendshipRepository = friendshipRepository;
        this.notifications = notifications;
    }

    // criar cobranca

    @Transactional
    public Payment createPayment(Long creatorId, BigDecimal amount, String description, List<Long> userIds) {
        // Dividir o total pelos usuários e criar uma cobrança para cada um

        if (userIds == null || userIds.isEmpty()) {
            throw new InvalidRequestException("User IDs list cannot be null or empty");
        }

        // ids repetidos gerariam duas linhas com a mesma chave (payment_id, user_id)
        if (new HashSet<>(userIds).size() != userIds.size()) {
            throw new InvalidRequestException("User IDs list cannot contain duplicates");
        }

        User creator = userRepository.findById(creatorId)
            .orElseThrow(() -> new NotFoundException("User " + creatorId + " not found"));

        // só amigos podem ser cobrados; a mesma mensagem vale para id inexistente,
        // para não revelar quais ids existem
        Set<Long> friendIds = new HashSet<>(friendshipRepository.findFriendIdsAmong(creatorId, userIds));
        for (Long userId : userIds) {
            if (!friendIds.contains(userId)) {
                throw new InvalidRequestException("User " + userId + " is not your friend");
            }
        }

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        Payment payment = new Payment(amount, description, creator);

        // o criador também entra na divisão (n + 1 partes), mas não gera linha em payment_user.
        // divide() sem escala lança ArithmeticException em dízimas (ex.: 100 / 3),
        // então arredonda para centavos e distribui a sobra entre os primeiros usuários
        BigDecimal parts = BigDecimal.valueOf(userIds.size() + 1);
        BigDecimal individualAmount = amount.divide(parts, 2, RoundingMode.DOWN);
        // a sobra é só o que o arredondamento deixou de fora de todas as partes (menos de n + 1 centavos)
        BigDecimal remainder = amount.subtract(individualAmount.multiply(parts));
        BigDecimal cent = new BigDecimal("0.01");

        for (Long userId : userIds) {
            User user = usersById.get(userId);
            if (user == null) {
                throw new InvalidRequestException("User " + userId + " not found");
            }

            BigDecimal share = individualAmount;
            if (remainder.compareTo(BigDecimal.ZERO) > 0) {
                share = share.add(cent);
                remainder = remainder.subtract(cent);
            }
            payment.addPaymentUser(new PaymentUser(user, share));
        }

        return paymentRepository.save(payment);
    }



    //Pagar

    @Transactional
    public PaymentUser pay(Long paymentId, Long userId) {
        PaymentUser paymentUserToPay = paymentUserRepository.findById(new PaymentUserId(paymentId, userId))
            // 404 tanto para cobrança inexistente quanto para cobrança de outra pessoa,
            // para não revelar quais ids existem
            .orElseThrow(() -> new NotFoundException("Payment " + paymentId + " not found"));

        if (paymentUserToPay.isPaid()) {
            throw new ConflictException("Payment already done");
        }

        // TODO: exibir o pix (paymentKey do criador) e validar o comprovante antes de marcar como paga

        paymentUserToPay.setPaid(true);
        paymentUserToPay.setPaidAt(OffsetDateTime.now());
        paymentUserRepository.save(paymentUserToPay);

        notifications.forEach(notification -> notification.notifyPayment(paymentUserToPay));

        return paymentUserToPay;
    }


    //Ver quem pagou

    // só o criador vê a cobrança inteira, com a parte e o status de cada participante
    @Transactional(readOnly = true)
    public List<Payment> createdPayments(Long creatorId) {
        return paymentRepository.findCreatedWithShares(creatorId);
    }

    @Transactional(readOnly = true)
    public Payment createdPayment(Long paymentId, Long creatorId) {
        // 404 também para cobrança de outra pessoa, para não revelar quais ids existem
        return paymentRepository.findCreatedWithShares(paymentId, creatorId)
            .orElseThrow(() -> new NotFoundException("Payment " + paymentId + " not found"));
    }

    // as partes que o usuário deve, pendentes primeiro; não inclui as dos outros participantes
    @Transactional(readOnly = true)
    public List<PaymentUser> owedShares(Long userId) {
        return paymentUserRepository.findOwedBy(userId);
    }

}
