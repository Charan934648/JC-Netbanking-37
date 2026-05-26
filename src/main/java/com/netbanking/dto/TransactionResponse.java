package com.netbanking.dto;

import com.netbanking.entity.Transaction;
import java.math.BigDecimal;

public record TransactionResponse(
    String transactionReference,
    String sourceAccountNumber,
    String targetAccountNumber,
    BigDecimal amount,
    String transferType,
    String status,
    String description,
    String timestamp
) {
    public static TransactionResponse fromEntity(Transaction transaction) {
        return new TransactionResponse(
            transaction.getTransactionReference(),
            transaction.getSourceAccount() != null ? transaction.getSourceAccount().getAccountNumber() : null,
            transaction.getTargetAccount() != null ? transaction.getTargetAccount().getAccountNumber() : null,
            transaction.getAmount(),
            transaction.getTransferType().name(),
            transaction.getStatus().name(),
            transaction.getDescription(),
            transaction.getTimestamp().toString()
        );
    }
}
