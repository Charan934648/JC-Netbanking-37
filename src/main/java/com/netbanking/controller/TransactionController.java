package com.netbanking.controller;

import com.netbanking.dto.TransactionResponse;
import com.netbanking.dto.TransferRequest;
import com.netbanking.entity.Transaction;
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
    public ResponseEntity<TransactionResponse> transferFunds(
            @Valid @RequestBody TransferRequest transferRequest,
            Principal principal,
            HttpServletRequest request) {

        TransferType type = TransferType.valueOf(transferRequest.transferType().toUpperCase());
        
        Transaction transaction = transactionService.transferFunds(
                transferRequest.sourceAccountNumber(),
                transferRequest.targetAccountNumber(),
                transferRequest.amount(),
                type,
                transferRequest.description() != null ? transferRequest.description() : "Fund Transfer via " + type,
                principal.getName(),
                request.getRemoteAddr()
        );

        return ResponseEntity.ok(TransactionResponse.fromEntity(transaction));
    }
}
