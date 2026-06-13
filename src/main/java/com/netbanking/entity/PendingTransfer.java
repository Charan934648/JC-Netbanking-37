package com.netbanking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_transfers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_account_number", nullable = false, length = 10)
    private String sourceAccountNumber;

    @Column(name = "target_account_number", nullable = false, length = 10)
    private String targetAccountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false)
    private TransferType transferType;

    private String description;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PendingTransferStatus status = PendingTransferStatus.PENDING_ADMIN_APPROVAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "transaction_reference")
    private String transactionReference;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
