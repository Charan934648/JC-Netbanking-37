package com.netbanking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_otp_username", columnList = "username")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'LOGIN'")
    @Builder.Default
    private String purpose = "LOGIN";

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Column(nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private int attempts = 0;

    @Builder.Default
    private boolean verified = false;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
}
