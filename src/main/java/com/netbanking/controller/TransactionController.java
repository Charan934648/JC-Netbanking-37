package com.netbanking.controller;

import com.netbanking.dto.TransactionResponse;
import com.netbanking.dto.TransferRequest;
import com.netbanking.dto.TransferSubmissionResponse;
import com.netbanking.entity.TransferType;
import com.netbanking.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransferSubmissionResponse> transferFunds(
            @Valid @RequestBody TransferRequest transferRequest,
            Principal principal,
            HttpServletRequest request) {

        TransferType type = TransferType.valueOf(transferRequest.transferType().toUpperCase());
        
        TransactionService.TransferSubmissionResult result = transactionService.submitTransfer(
                transferRequest.sourceAccountNumber(),
                transferRequest.targetAccountNumber(),
                transferRequest.amount(),
                type,
                transferRequest.description() != null ? transferRequest.description() : "Fund Transfer via " + type,
                transferRequest.beneficiaryId(),
                transferRequest.otpCode(),
                principal.getName(),
                request.getRemoteAddr()
        );

        if ("OTP_REQUIRED".equals(result.status())) {
            return ResponseEntity.ok(TransferSubmissionResponse.otpRequired());
        }
        if ("PENDING_ADMIN_APPROVAL".equals(result.status())) {
            return ResponseEntity.ok(TransferSubmissionResponse.pendingApproval(result.pendingTransfer().getId()));
        }

        return ResponseEntity.ok(TransferSubmissionResponse.completed(TransactionResponse.fromEntity(result.transaction())));
    }
}
