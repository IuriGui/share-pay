package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.demo.exception.InvalidRequestException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Friendship;
import com.example.demo.model.User;
import com.example.demo.repository.FriendshipRepository;
import com.example.demo.repository.UserRepository;

class FriendServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final FriendshipRepository friendshipRepository = mock(FriendshipRepository.class);

    private final FriendService friendService = new FriendService(userRepository, friendshipRepository);

    private final User me = user(1L);
    private final User other = user(2L);

    @BeforeEach
    void setUp() {
        when(userRepository.findById(me.getId())).thenReturn(Optional.of(me));
        when(userRepository.findByEmail(other.getEmail())).thenReturn(Optional.of(other));
    }

    @Test
    void requestCreatesPendingFriendship() {
        friendService.requestFriendship(me.getId(), "  USER2@example.com ");

        ArgumentCaptor<Friendship> saved = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).save(saved.capture());
        assertThat(saved.getValue().getRequester()).isSameAs(me);
        assertThat(saved.getValue().getAddressee()).isSameAs(other);
        assertThat(saved.getValue().isAccepted()).isFalse();
    }

    @Test
    void requestToUnknownEmailSucceedsSilently() {
        friendService.requestFriendship(me.getId(), "ninguem@example.com");

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void requestToYourselfIsRejected() {
        assertThatThrownBy(() -> friendService.requestFriendship(me.getId(), me.getEmail()))
            .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void requestBackAcceptsTheOtherPersonsPendingRequest() {
        Friendship theirRequest = new Friendship(other, me);
        when(friendshipRepository.findBetween(me.getId(), other.getId())).thenReturn(Optional.of(theirRequest));

        friendService.requestFriendship(me.getId(), other.getEmail());

        assertThat(theirRequest.isAccepted()).isTrue();
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void repeatingYourOwnRequestDoesNotAcceptIt() {
        Friendship myRequest = new Friendship(me, other);
        when(friendshipRepository.findBetween(me.getId(), other.getId())).thenReturn(Optional.of(myRequest));

        friendService.requestFriendship(me.getId(), other.getEmail());

        assertThat(myRequest.isAccepted()).isFalse();
    }

    @Test
    void friendsReturnsTheOtherSideOfEachFriendship() {
        Friendship sent = accepted(new Friendship(me, other));
        Friendship received = accepted(new Friendship(user(3L), me));
        when(friendshipRepository.findAcceptedOf(me.getId())).thenReturn(List.of(sent, received));

        assertThat(friendService.friends(me.getId())).extracting(User::getId).containsExactly(2L, 3L);
    }

    @Test
    void acceptOnlyWorksForRequestsSentToYou() {
        Friendship request = new Friendship(other, me);
        when(friendshipRepository.findPending(other.getId(), me.getId())).thenReturn(Optional.of(request));

        assertThat(friendService.accept(me.getId(), other.getId())).isSameAs(other);
        assertThat(request.isAccepted()).isTrue();

        // quem enviou não pode aceitar o próprio pedido
        assertThatThrownBy(() -> friendService.accept(other.getId(), me.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void removeRejectsPendingRequests() {
        when(friendshipRepository.findBetween(me.getId(), other.getId())).thenReturn(Optional.of(new Friendship(me, other)));

        assertThatThrownBy(() -> friendService.remove(me.getId(), other.getId())).isInstanceOf(NotFoundException.class);
        verify(friendshipRepository, never()).delete(any());
    }

    private static Friendship accepted(Friendship friendship) {
        friendship.setAcceptedAt(OffsetDateTime.now());
        return friendship;
    }

    private static User user(Long id) {
        User user = new User("user" + id, "user" + id + "@example.com", "secret", null);
        user.setId(id);
        return user;
    }
}
