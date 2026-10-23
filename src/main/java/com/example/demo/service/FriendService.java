package com.example.demo.service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Friendship;
import com.example.demo.model.User;
import com.example.demo.repository.FriendshipRepository;
import com.example.demo.repository.UserRepository;

@Service
public class FriendService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public FriendService(UserRepository userRepository, FriendshipRepository friendshipRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    // o resultado é o mesmo para email cadastrado ou não, para não permitir enumerar usuários;
    // os dados de quem recebe o pedido só aparecem para quem pediu depois que ele aceitar
    @Transactional
    public void requestFriendship(Long userId, String email) {
        User requester = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.equals(requester.getEmail())) {
            throw new InvalidRequestException("You cannot add yourself");
        }

        Optional<User> addressee = userRepository.findByEmail(normalizedEmail);
        if (addressee.isEmpty()) {
            return;
        }

        Optional<Friendship> existing = friendshipRepository.findBetween(userId, addressee.get().getId());
        if (existing.isEmpty()) {
            friendshipRepository.save(new Friendship(requester, addressee.get()));
            return;
        }

        // a outra pessoa já tinha pedido: os dois querem, então vira amizade
        Friendship friendship = existing.get();
        if (!friendship.isAccepted() && friendship.getAddressee().getId().equals(userId)) {
            friendship.setAcceptedAt(OffsetDateTime.now());
        }
    }

    @Transactional(readOnly = true)
    public List<User> friends(Long userId) {
        return friendshipRepository.findAcceptedOf(userId).stream()
            .map(friendship -> friendship.other(userId))
            .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<User> pendingRequests(Long userId) {
        return friendshipRepository.findPendingTo(userId).stream().map(Friendship::getRequester).toList();
    }

    @Transactional
    public User accept(Long userId, Long requesterId) {
        Friendship friendship = pendingRequest(userId, requesterId);
        friendship.setAcceptedAt(OffsetDateTime.now());
        return friendship.getRequester();
    }

    @Transactional
    public void decline(Long userId, Long requesterId) {
        friendshipRepository.delete(pendingRequest(userId, requesterId));
    }

    @Transactional
    public void remove(Long userId, Long friendId) {
        Friendship friendship = friendshipRepository.findBetween(userId, friendId)
            .filter(Friendship::isAccepted)
            .orElseThrow(() -> new NotFoundException("Friend " + friendId + " not found"));
        friendshipRepository.delete(friendship);
    }

    private Friendship pendingRequest(Long userId, Long requesterId) {
        return friendshipRepository.findPending(requesterId, userId)
            .orElseThrow(() -> new NotFoundException("Friend request not found"));
    }
}
