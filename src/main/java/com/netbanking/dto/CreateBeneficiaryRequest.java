package com.netbanking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateBeneficiaryRequest(
        @NotBlank(message = "Nickname is required")
        String nickname,

        @NotBlank(message = "Bank name is required")
        String bankName,

        @NotBlank(message = "IFSC code is required")
        @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC code must be valid")
        String ifscCode,

        @NotBlank(message = "Account number is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Account number must be 10 digits")
        String accountNumber
) {}
