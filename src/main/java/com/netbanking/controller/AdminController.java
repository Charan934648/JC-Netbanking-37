package com.netbanking.controller;

import com.netbanking.dto.PendingTransferResponse;
import com.netbanking.entity.AuditLog;
import com.netbanking.entity.PendingTransfer;
import com.netbanking.service.AuditLogService;
import com.netbanking.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuditLogService auditLogService;
    private final TransactionService transactionService;

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        List<AuditLog> logs = auditLogService.getAllLogs();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/pending-transfers")
    public ResponseEntity<List<PendingTransferResponse>> getPendingTransfers() {
        return ResponseEntity.ok(transactionService.getPendingTransfersForApproval().stream()
                .map(PendingTransferResponse::fromEntity)
                .toList());
    }

    @PostMapping("/pending-transfers/{id}/approve")
    public ResponseEntity<PendingTransferResponse> approvePendingTransfer(
            @PathVariable Long id,
            Principal principal,
            HttpServletRequest request) {
        PendingTransfer pendingTransfer = transactionService.approvePendingTransfer(id, principal.getName(), request.getRemoteAddr());
        return ResponseEntity.ok(PendingTransferResponse.fromEntity(pendingTransfer));
    }
}
