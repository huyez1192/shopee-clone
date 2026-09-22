package com.ecommerce.minishopee.payment.repository;

import com.ecommerce.minishopee.payment.entity.SePayTransaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SePayTransactionRepository extends JpaRepository<SePayTransaction, Long> {

    Optional<SePayTransaction> findBySePayTransactionId(Long sePayTransactionId);
}
