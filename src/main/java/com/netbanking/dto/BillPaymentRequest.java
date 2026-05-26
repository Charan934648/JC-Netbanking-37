package com.netbanking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BillPaymentRequest(
    @NotBlank(message = "Account number is required")
    String accountNumber,

    @NotBlank(message = "Biller name is required")
    String billerName,

    @NotBlank(message = "Consumer reference number is required")
    String consumerNumber,

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "1.00", message = "Minimum payment amount is 1.00")
    BigDecimal amount
) {}
