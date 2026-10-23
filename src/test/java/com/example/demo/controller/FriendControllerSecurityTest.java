package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.model.User;
import com.example.demo.security.SecurityConfig;
import com.example.demo.service.FriendService;

@WebMvcTest(
    controllers = FriendController.class,
    properties = "security.jwt.secret=dGVzdC1zZWNyZXQtd2l0aC1hdC1sZWFzdC0yNTYtYml0cyE="
)
@Import(SecurityConfig.class)
class FriendControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FriendService friendService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/friends")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/friends/requests").contentType(MediaType.APPLICATION_JSON).content("""
                {"email": "a@example.com"}
                """))
            .andExpect(status().isUnauthorized());

        verify(friendService, never()).requestFriendship(any(), any());
    }

    @Test
    void requestAnswersTheSameWhetherOrNotTheEmailExists() throws Exception {
        mockMvc.perform(post("/api/friends/requests")
                .with(jwt().jwt(token -> token.subject("7")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "ninguem@example.com"}
                    """))
            .andExpect(status().isAccepted())
            .andExpect(content().string(""));

        verify(friendService).requestFriendship(7L, "ninguem@example.com");
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/friends/requests")
                .with(jwt().jwt(token -> token.subject("7")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email": "nao-e-email"}
                    """))
            .andExpect(status().isBadRequest());

        verify(friendService, never()).requestFriendship(any(), any());
    }

    @Test
    void listsFriendsWithoutSensitiveFields() throws Exception {
        User friend = new User("ana", "ana@example.com", "{bcrypt}hash", "pix-ana");
        friend.setId(2L);
        when(friendService.friends(7L)).thenReturn(List.of(friend));

        mockMvc.perform(get("/api/friends").with(jwt().jwt(token -> token.subject("7"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("ana@example.com"))
            .andExpect(jsonPath("$[0].password").doesNotExist())
            .andExpect(jsonPath("$[0].paymentKey").doesNotExist());
    }
}
