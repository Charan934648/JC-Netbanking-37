package com.netbanking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries", indexes = {
        @Index(name = "idx_beneficiary_user", columnList = "user_id"),
        @Index(name = "idx_beneficiary_account", columnList = "account_number")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "ifsc_code", nullable = false)
    private String ifscCode;

    @Column(name = "account_number", nullable = false, length = 10)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BeneficiaryStatus status = BeneficiaryStatus.PENDING;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.availableAt == null) {
            this.availableAt = LocalDateTime.now();
        }
    }

    public boolean isReadyForTransfers() {
        return status == BeneficiaryStatus.ACTIVE && !LocalDateTime.now().isBefore(availableAt);
    }
}
