package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.exception.ConflictException;
import com.example.demo.exception.InvalidRequestException;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.TokenService;
import com.example.demo.security.TokenService.IssuedToken;

@Service
public class AuthService {

    // o bcrypt só considera os primeiros 72 bytes da senha
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Transactional
    public User register(String name, String email, String rawPassword, String paymentKey) {
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new InvalidRequestException("Password must have at most " + BCRYPT_MAX_BYTES + " bytes");
        }

        String normalizedEmail = normalizeEmail(email);
        // mensagem genérica para não confirmar qual dos dois já está cadastrado
        if (userRepository.existsByEmail(normalizedEmail) || userRepository.existsByName(name)) {
            throw new ConflictException("Email or username already in use");
        }

        User user = new User(name, normalizedEmail, passwordEncoder.encode(rawPassword), paymentKey);
        return userRepository.save(user);
    }

    public IssuedToken login(String email, String rawPassword) {
        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(normalizeEmail(email), rawPassword));

        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return tokenService.issue(user);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
