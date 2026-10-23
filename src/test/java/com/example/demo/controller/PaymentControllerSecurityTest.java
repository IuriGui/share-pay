package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentUser;
import com.example.demo.model.User;
import com.example.demo.security.SecurityConfig;
import com.example.demo.service.UserService;

@WebMvcTest(
    controllers = PaymentController.class,
    properties = "security.jwt.secret=dGVzdC1zZWNyZXQtd2l0aC1hdC1sZWFzdC0yNTYtYml0cyE="
)
@Import(SecurityConfig.class)
class PaymentControllerSecurityTest {

    private static final String CREATE_BODY = """
        {"amount": 30.00, "description": "jantar", "userIds": [2, 3]}
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
            .andExpect(status().isUnauthorized());

        verify(userService, never()).createPayment(any(), any(), any(), any());
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        mockMvc.perform(post("/api/payments/1/pay").header("Authorization", "Bearer not-a-jwt"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createsPaymentForAuthenticatedUser() throws Exception {
        User creator = user(7L);
        Payment payment = new Payment(new BigDecimal("30.00"), "jantar", creator);
        payment.setId(1L);
        payment.addPaymentUser(new PaymentUser(user(2L), new BigDecimal("15.00")));
        payment.addPaymentUser(new PaymentUser(user(3L), new BigDecimal("15.00")));
        when(userService.createPayment(eq(7L), any(), eq("jantar"), eq(List.of(2L, 3L)))).thenReturn(payment);

        mockMvc.perform(post("/api/payments")
                .with(jwt().jwt(token -> token.subject("7")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.createdBy").value(7))
            .andExpect(jsonPath("$.shares.length()").value(2))
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void paysOnlyTheAuthenticatedUsersShare() throws Exception {
        Payment payment = new Payment(new BigDecimal("15.00"), "jantar", user(1L));
        payment.setId(5L);
        PaymentUser share = new PaymentUser(user(7L), new BigDecimal("15.00"));
        share.setPaid(true);
        payment.addPaymentUser(share);
        when(userService.pay(5L, 7L)).thenReturn(share);

        mockMvc.perform(post("/api/payments/5/pay").with(jwt().jwt(token -> token.subject("7"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(7))
            .andExpect(jsonPath("$.paid").value(true));

        verify(userService).pay(5L, 7L);
    }

    @Test
    void returnsNotFoundForSomeoneElsesPayment() throws Exception {
        when(userService.pay(anyLong(), anyLong())).thenThrow(new NotFoundException("Payment 5 not found"));

        mockMvc.perform(post("/api/payments/5/pay").with(jwt().jwt(token -> token.subject("7"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void listsOwedSharesWithCreatorsPixButNotOtherParticipants() throws Exception {
        User creator = user(1L);
        Payment payment = new Payment(new BigDecimal("30.00"), "jantar", creator);
        payment.setId(5L);
        PaymentUser mine = new PaymentUser(user(7L), new BigDecimal("10.00"));
        payment.addPaymentUser(mine);
        payment.addPaymentUser(new PaymentUser(user(8L), new BigDecimal("10.00")));
        when(userService.owedShares(7L)).thenReturn(List.of(mine));

        mockMvc.perform(get("/api/payments/owed").with(jwt().jwt(token -> token.subject("7"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].paymentId").value(5))
            .andExpect(jsonPath("$[0].creatorName").value("user1"))
            .andExpect(jsonPath("$[0].creatorPaymentKey").value("pix-1"))
            .andExpect(jsonPath("$[0].amount").value(10.00))
            .andExpect(jsonPath("$[0].shares").doesNotExist())
            .andExpect(jsonPath("$[0].creatorEmail").doesNotExist());
    }

    @Test
    void listsCreatedPaymentsWithParticipantNames() throws Exception {
        Payment payment = new Payment(new BigDecimal("30.00"), "jantar", user(7L));
        payment.setId(5L);
        payment.addPaymentUser(new PaymentUser(user(2L), new BigDecimal("10.00")));
        when(userService.createdPayments(7L)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/payments/created").with(jwt().jwt(token -> token.subject("7"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].shares[0].userName").value("user2"))
            .andExpect(jsonPath("$[0].shares[0].email").doesNotExist());
    }

    @Test
    void returnsNotFoundForPaymentCreatedBySomeoneElse() throws Exception {
        when(userService.createdPayment(5L, 7L)).thenThrow(new NotFoundException("Payment 5 not found"));

        mockMvc.perform(get("/api/payments/5").with(jwt().jwt(token -> token.subject("7"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectsListingWithoutToken() throws Exception {
        mockMvc.perform(get("/api/payments/owed")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/payments/created")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/api/payments")
                .with(jwt().jwt(token -> token.subject("7")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"amount": -1, "userIds": []}
                    """))
            .andExpect(status().isBadRequest());

        verify(userService, never()).createPayment(any(), any(), any(), any());
    }

    @Test
    void sendsSecurityHeaders() throws Exception {
        mockMvc.perform(post("/api/payments/5/pay"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(header().exists("Cache-Control"));
    }

    private static User user(Long id) {
        User user = new User("user" + id, "user" + id + "@example.com", "{bcrypt}hash", "pix-" + id);
        user.setId(id);
        return user;
    }
}
