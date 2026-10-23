package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.PaymentUser;
import com.example.demo.model.PaymentUserId;

public interface PaymentUserRepository extends JpaRepository<PaymentUser, PaymentUserId> {

    List<PaymentUser> findByPaymentId(Long paymentId);

    List<PaymentUser> findByPaymentIdAndPaid(Long paymentId, boolean paid);

    List<PaymentUser> findByUserIdAndPaidFalse(Long userId);

    @Query("""
        select pu from PaymentUser pu join fetch pu.payment p join fetch p.createdBy
        where pu.user.id = :userId
        order by pu.paid asc, p.createdAt desc
        """)
    List<PaymentUser> findOwedBy(@Param("userId") Long userId);
}
