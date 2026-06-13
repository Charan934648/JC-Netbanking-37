package com.netbanking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RealtimeEventResponse(
        String eventType,
        String accountNumber,
        String transactionReference,
        BigDecimal balance,
        BigDecimal amount,
        String transferType,
        String description,
        String timestamp
) {
    public static RealtimeEventResponse of(
            String eventType,
            String accountNumber,
            String transactionReference,
            BigDecimal balance,
            BigDecimal amount,
            String transferType,
            String description) {
        return new RealtimeEventResponse(
                eventType,
                accountNumber,
                transactionReference,
                balance,
                amount,
                transferType,
                description,
                LocalDateTime.now().toString()
        );
    }
}
