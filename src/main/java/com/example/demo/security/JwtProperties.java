package com.example.demo.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("security.jwt")
public record JwtProperties(
    // chave HMAC em Base64 com pelo menos 256 bits (ex.: openssl rand -base64 32)
    String secret,
    @DefaultValue("share-pay") String issuer,
    @DefaultValue("1h") Duration expiration
) {
}
