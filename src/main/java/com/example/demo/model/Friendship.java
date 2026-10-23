package com.example.demo.model;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "friendship")
@Getter
@Setter
@NoArgsConstructor
public class Friendship {

    @EmbeddedId
    private FriendshipId id = new FriendshipId();

    @MapsId("requesterId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    @MapsId("addresseeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id")
    private User addressee;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // nulo enquanto o pedido não foi aceito
    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    public Friendship(User requester, User addressee) {
        this.requester = requester;
        this.addressee = addressee;
    }

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    public User other(Long userId) {
        return requester.getId().equals(userId) ? addressee : requester;
    }
}
