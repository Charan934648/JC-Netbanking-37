package com.netbanking.dto;

import com.netbanking.entity.Beneficiary;

public record BeneficiaryResponse(
        Long id,
        String nickname,
        String bankName,
        String ifscCode,
        String accountNumber,
        String status,
        boolean readyForTransfers,
        String availableAt,
        String verifiedAt
) {
    public static BeneficiaryResponse fromEntity(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getNickname(),
                beneficiary.getBankName(),
                beneficiary.getIfscCode(),
                beneficiary.getAccountNumber(),
                beneficiary.getStatus().name(),
                beneficiary.isReadyForTransfers(),
                beneficiary.getAvailableAt().toString(),
                beneficiary.getVerifiedAt() != null ? beneficiary.getVerifiedAt().toString() : null
        );
    }
}
