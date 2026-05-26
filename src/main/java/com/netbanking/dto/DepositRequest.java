package com.netbanking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(
    @NotBlank(message = "Account number is required")
    String accountNumber,

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "1.00", message = "Minimum deposit amount is 1.00")
    BigDecimal amount
) {}
