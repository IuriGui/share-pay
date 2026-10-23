package com.example.demo.controller.dto;

import com.example.demo.model.User;

// nunca expor a entidade diretamente: ela carrega o hash da senha
public record UserResponse(Long id, String name, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
