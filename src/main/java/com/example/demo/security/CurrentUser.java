package com.example.demo.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentUser {

    private CurrentUser() {
    }

    // o id do usuário vem sempre do token assinado, nunca do corpo ou da URL da requisição
    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
