package com.example.demo.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // o login é feito por email
    @Override
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(email)
            .map(user -> User.withUsername(user.getEmail()).password(user.getPassword()).build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
