package com.ecommerce.minishopee.payment.repository;

import com.ecommerce.minishopee.payment.entity.Payment;
import com.ecommerce.minishopee.payment.enums.PaymentSessionStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByPaymentCode(String paymentCode);

    Optional<Payment> findByPaymentCode(String paymentCode);

    Optional<Payment> findByPaymentCodeAndUserId(String paymentCode, Long userId);


    @Query("""
            SELECT p.id
            FROM Payment p
            WHERE p.status = :status
              AND p.expiresAt <= :now
            """)
    List<Long> findExpiredPaymentIds(
            @Param("status") PaymentSessionStatus status,
            @Param("now") LocalDateTime now
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.id = :paymentId
            """)
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.paymentCode = :paymentCode
            """)
    Optional<Payment> findByPaymentCodeForUpdate(@Param("paymentCode") String paymentCode);
}