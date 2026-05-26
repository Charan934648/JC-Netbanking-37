package com.netbanking.dto;

import com.netbanking.entity.Account;
import java.math.BigDecimal;

public record AccountResponse(
    String accountNumber,
    BigDecimal balance,
    String accountType,
    String ownerName,
    String createdAt
) {
    public static AccountResponse fromEntity(Account account) {
        return new AccountResponse(
            account.getAccountNumber(),
            account.getBalance(),
            account.getAccountType(),
            account.getUser().getUsername(),
            account.getCreatedAt().toString()
        );
    }
}
