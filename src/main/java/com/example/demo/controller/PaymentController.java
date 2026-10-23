package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.controller.dto.CreatePaymentRequest;
import com.example.demo.controller.dto.OwedShareResponse;
import com.example.demo.controller.dto.PaymentResponse;
import com.example.demo.controller.dto.PaymentShareResponse;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Cobranças divididas entre usuários")
public class PaymentController {

    private final UserService userService;

    public PaymentController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Cria uma cobrança dividida entre os usuários informados; o criador é o usuário do token")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreatePaymentRequest request) {
        return PaymentResponse.from(userService.createPayment(
            CurrentUser.id(jwt), request.amount(), request.description(), request.userIds()));
    }

    @Operation(summary = "Lista as cobranças criadas pelo usuário do token, com a parte de cada participante")
    @GetMapping("/created")
    public List<PaymentResponse> created(@AuthenticationPrincipal Jwt jwt) {
        return userService.createdPayments(CurrentUser.id(jwt)).stream().map(PaymentResponse::from).toList();
    }

    @Operation(summary = "Lista as partes que o usuário do token deve, pendentes primeiro")
    @GetMapping("/owed")
    public List<OwedShareResponse> owed(@AuthenticationPrincipal Jwt jwt) {
        return userService.owedShares(CurrentUser.id(jwt)).stream().map(OwedShareResponse::from).toList();
    }

    @Operation(summary = "Detalha uma cobrança criada pelo usuário do token")
    @GetMapping("/{paymentId}")
    public PaymentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long paymentId) {
        return PaymentResponse.from(userService.createdPayment(paymentId, CurrentUser.id(jwt)));
    }

    // só dá para pagar a própria parte: o usuário vem do token, não da URL
    @Operation(summary = "Marca como paga a parte do usuário do token na cobrança")
    @PostMapping("/{paymentId}/pay")
    public PaymentShareResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable Long paymentId) {
        return PaymentShareResponse.from(userService.pay(paymentId, CurrentUser.id(jwt)));
    }
}
