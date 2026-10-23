package com.example.demo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Friendship;
import com.example.demo.model.FriendshipId;

public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

    @Query("""
        select f from Friendship f
        where (f.requester.id = :a and f.addressee.id = :b) or (f.requester.id = :b and f.addressee.id = :a)
        """)
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    // join fetch porque o controller lê nome e email fora da transação (open-in-view desligado)
    @Query("""
        select f from Friendship f join fetch f.requester join fetch f.addressee
        where (f.requester.id = :userId or f.addressee.id = :userId) and f.acceptedAt is not null
        """)
    List<Friendship> findAcceptedOf(@Param("userId") Long userId);

    @Query("""
        select f from Friendship f join fetch f.requester
        where f.addressee.id = :userId and f.acceptedAt is null
        order by f.createdAt desc
        """)
    List<Friendship> findPendingTo(@Param("userId") Long userId);

    @Query("""
        select f from Friendship f join fetch f.requester
        where f.requester.id = :requesterId and f.addressee.id = :addresseeId and f.acceptedAt is null
        """)
    Optional<Friendship> findPending(@Param("requesterId") Long requesterId, @Param("addresseeId") Long addresseeId);

    @Query("""
        select case when f.requester.id = :userId then f.addressee.id else f.requester.id end
        from Friendship f
        where f.acceptedAt is not null
          and ((f.requester.id = :userId and f.addressee.id in :ids) or (f.addressee.id = :userId and f.requester.id in :ids))
        """)
    List<Long> findFriendIdsAmong(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
