package com.example.demo.controller.dto;

import java.time.Instant;

import com.example.demo.security.TokenService.IssuedToken;

public record TokenResponse(String token, String type, Instant expiresAt) {

    public static TokenResponse from(IssuedToken issuedToken) {
        return new TokenResponse(issuedToken.token(), "Bearer", issuedToken.expiresAt());
    }
}
