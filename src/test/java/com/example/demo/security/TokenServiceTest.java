package com.example.demo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import com.example.demo.model.User;
import com.example.demo.security.TokenService.IssuedToken;

class TokenServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQtd2l0aC1hdC1sZWFzdC0yNTYtYml0cyE=";

    private final SecurityConfig config = new SecurityConfig();
    private final JwtProperties properties = new JwtProperties(SECRET, "share-pay", Duration.ofHours(1));
    private final SecretKey key = config.jwtSecretKey(properties);
    private final TokenService tokenService = new TokenService(config.jwtEncoder(key), properties);
    private final JwtDecoder decoder = config.jwtDecoder(key, properties);

    @Test
    void issuedTokenCarriesUserIdAsSubject() {
        IssuedToken issued = tokenService.issue(user(42L));

        assertThat(decoder.decode(issued.token()).getSubject()).isEqualTo("42");
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtProperties otherProperties = new JwtProperties(
            "b3V0cmEtY2hhdmUtc2VjcmV0YS1kZS0yNTYtYml0cy0hIQ==", "share-pay", Duration.ofHours(1));
        TokenService otherIssuer = new TokenService(
            config.jwtEncoder(config.jwtSecretKey(otherProperties)), otherProperties);

        String forged = otherIssuer.issue(user(42L)).token();

        assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        JwtProperties otherIssuerProperties = new JwtProperties(SECRET, "someone-else", Duration.ofHours(1));
        String token = new TokenService(config.jwtEncoder(key), otherIssuerProperties).issue(user(42L)).token();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        Instant issuedAt = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("share-pay")
            .subject("42")
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(Duration.ofHours(1)))
            .build();
        String token = config.jwtEncoder(key)
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void refusesShortSecret() {
        JwtProperties weak = new JwtProperties("c2hvcnQ=", "share-pay", Duration.ofHours(1));

        assertThatThrownBy(() -> config.jwtSecretKey(weak)).isInstanceOf(IllegalStateException.class);
    }

    private static User user(Long id) {
        User user = new User("user" + id, "user" + id + "@example.com", "{bcrypt}hash", null);
        user.setId(id);
        return user;
    }
}
