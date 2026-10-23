package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByCreatedById(Long userId);

    // join fetch porque o controller lê os participantes fora da transação (open-in-view desligado)
    @Query("""
        select distinct p from Payment p left join fetch p.payments pu left join fetch pu.user
        where p.createdBy.id = :creatorId
        order by p.createdAt desc
        """)
    List<Payment> findCreatedWithShares(@Param("creatorId") Long creatorId);

    @Query("""
        select p from Payment p left join fetch p.payments pu left join fetch pu.user
        where p.id = :paymentId and p.createdBy.id = :creatorId
        """)
    Optional<Payment> findCreatedWithShares(@Param("paymentId") Long paymentId, @Param("creatorId") Long creatorId);
}
