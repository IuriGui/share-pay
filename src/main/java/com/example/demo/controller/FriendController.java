package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.controller.dto.FriendRequestBody;
import com.example.demo.controller.dto.UserResponse;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.FriendService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/friends")
@Tag(name = "Friends", description = "Amigos: só eles podem ser incluídos numa cobrança")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @Operation(summary = "Lista os amigos do usuário do token")
    @GetMapping
    public List<UserResponse> friends(@AuthenticationPrincipal Jwt jwt) {
        return friendService.friends(CurrentUser.id(jwt)).stream().map(UserResponse::from).toList();
    }

    // sempre 202, exista ou não uma conta com esse email
    @Operation(summary = "Envia pedido de amizade por email; a resposta não revela se o email está cadastrado")
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void request(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody FriendRequestBody body) {
        friendService.requestFriendship(CurrentUser.id(jwt), body.email());
    }

    @Operation(summary = "Lista os pedidos de amizade recebidos e ainda não respondidos")
    @GetMapping("/requests")
    public List<UserResponse> pendingRequests(@AuthenticationPrincipal Jwt jwt) {
        return friendService.pendingRequests(CurrentUser.id(jwt)).stream().map(UserResponse::from).toList();
    }

    @Operation(summary = "Aceita o pedido de amizade enviado pelo usuário informado")
    @PostMapping("/requests/{requesterId}/accept")
    public UserResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requesterId) {
        return UserResponse.from(friendService.accept(CurrentUser.id(jwt), requesterId));
    }

    @Operation(summary = "Recusa o pedido de amizade enviado pelo usuário informado")
    @DeleteMapping("/requests/{requesterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requesterId) {
        friendService.decline(CurrentUser.id(jwt), requesterId);
    }

    @Operation(summary = "Desfaz a amizade com o usuário informado")
    @DeleteMapping("/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long friendId) {
        friendService.remove(CurrentUser.id(jwt), friendId);
    }
}
