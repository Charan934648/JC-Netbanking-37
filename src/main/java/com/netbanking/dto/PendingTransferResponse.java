package com.netbanking.dto;

import com.netbanking.entity.PendingTransfer;

import java.math.BigDecimal;

public record PendingTransferResponse(
        Long id,
        String sourceAccountNumber,
        String targetAccountNumber,
        BigDecimal amount,
        String transferType,
        String description,
        String username,
        String status,
        String createdAt,
        String transactionReference
) {
    public static PendingTransferResponse fromEntity(PendingTransfer pendingTransfer) {
        return new PendingTransferResponse(
                pendingTransfer.getId(),
                pendingTransfer.getSourceAccountNumber(),
                pendingTransfer.getTargetAccountNumber(),
                pendingTransfer.getAmount(),
                pendingTransfer.getTransferType().name(),
                pendingTransfer.getDescription(),
                pendingTransfer.getUsername(),
                pendingTransfer.getStatus().name(),
                pendingTransfer.getCreatedAt().toString(),
                pendingTransfer.getTransactionReference()
        );
    }
}
