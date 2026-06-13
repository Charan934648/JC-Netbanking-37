package com.netbanking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record TransferRequest(
    @NotBlank(message = "Source account number is required")
    String sourceAccountNumber,

    @NotBlank(message = "Target account number is required")
    String targetAccountNumber,

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "1.00", message = "Minimum transfer amount is 1.00")
    BigDecimal amount,

    @NotBlank(message = "Transfer type is required")
    @Pattern(regexp = "(?i)^(NEFT|IMPS|RTGS)$", message = "Transfer type must be NEFT, IMPS, or RTGS")
    String transferType,

    Long beneficiaryId,

    @Pattern(regexp = "^$|^[0-9]{6}$", message = "OTP code must be a 6-digit number")
    String otpCode,

    String description
) {}
