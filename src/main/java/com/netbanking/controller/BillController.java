package com.netbanking.controller;

import com.netbanking.dto.BillPaymentRequest;
import com.netbanking.dto.TransactionResponse;
import com.netbanking.entity.Transaction;
import com.netbanking.service.BillPaymentService;
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
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillPaymentService billPaymentService;

    @PostMapping("/pay")
    public ResponseEntity<TransactionResponse> payBill(
            @Valid @RequestBody BillPaymentRequest billPaymentRequest,
            Principal principal,
            HttpServletRequest request) {

        Transaction transaction = billPaymentService.payBill(
                billPaymentRequest.accountNumber(),
                billPaymentRequest.billerName(),
                billPaymentRequest.consumerNumber(),
                billPaymentRequest.amount(),
                principal.getName(),
                request.getRemoteAddr()
        );

        return ResponseEntity.ok(TransactionResponse.fromEntity(transaction));
    }
}
