package com.ecommerce.minishopee.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "sepay_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sepay_transaction_id",
                        columnNames = "sepay_transaction_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_sepay_transactions_payment_id",
                        columnList = "payment_id"
                )
        }
)
public class SePayTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "sepay_transaction_id",
            nullable = false
    )
    private Long sePayTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(length = 100)
    private String gateway;

    @Column(
            name = "account_number",
            length = 100
    )
    private String accountNumber;

    @Column(
            name = "transfer_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal transferAmount;

    @Column(
            name = "reference_code",
            length = 255
    )
    private String referenceCode;

    @Column(
            name = "raw_payload",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String rawPayload;

    @Column(
            name = "received_at",
            nullable = false
    )
    private LocalDateTime receivedAt;

    @PrePersist
    public void prePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}