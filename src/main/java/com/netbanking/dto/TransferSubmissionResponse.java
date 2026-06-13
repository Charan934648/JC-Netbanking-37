package com.netbanking.dto;

public record TransferSubmissionResponse(
        String status,
        String message,
        Long approvalId,
        TransactionResponse transaction
) {
    public static TransferSubmissionResponse completed(TransactionResponse transaction) {
        return new TransferSubmissionResponse("COMPLETED", "Transfer completed successfully", null, transaction);
    }

    public static TransferSubmissionResponse otpRequired() {
        return new TransferSubmissionResponse("OTP_REQUIRED", "High-value transfer requires OTP verification", null, null);
    }

    public static TransferSubmissionResponse pendingApproval(Long approvalId) {
        return new TransferSubmissionResponse("PENDING_ADMIN_APPROVAL", "Transfer is waiting for admin approval", approvalId, null);
    }
}
