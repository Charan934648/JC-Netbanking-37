package com.netbanking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    String password,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid format")
    String email,

    @Pattern(regexp = "^$|^[0-9]{10,15}$", message = "Phone number must be 10 to 15 digits")
    String phoneNumber
) {}
