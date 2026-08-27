package com.ecommerce.minishopee.payment.repository;

import com.ecommerce.minishopee.payment.entity.SePayTransaction;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SePayTransactionRepository extends JpaRepository<SePayTransaction, Long> {

    boolean existsBySePayTransactionId(Long sePayTransactionId);
}