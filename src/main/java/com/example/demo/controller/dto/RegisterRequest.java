package com.example.demo.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Email @Size(max = 255) String email,
    // OWASP ASVS: mínimo de 12 caracteres, sem regras de composição
    @NotBlank @Size(min = 12, max = 72) String password,
    @Size(max = 255) String paymentKey
) {
}
