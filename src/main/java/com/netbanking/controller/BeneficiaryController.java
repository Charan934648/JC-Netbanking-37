package com.netbanking.controller;

import com.netbanking.dto.BeneficiaryResponse;
import com.netbanking.dto.CreateBeneficiaryRequest;
import com.netbanking.entity.Beneficiary;
import com.netbanking.service.BeneficiaryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> createBeneficiary(
            @Valid @RequestBody CreateBeneficiaryRequest request,
            Principal principal,
            HttpServletRequest httpRequest) {
        Beneficiary beneficiary = beneficiaryService.createBeneficiary(
                principal.getName(),
                request.nickname(),
                request.bankName(),
                request.ifscCode(),
                request.accountNumber(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(BeneficiaryResponse.fromEntity(beneficiary));
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> listBeneficiaries(Principal principal) {
        return ResponseEntity.ok(beneficiaryService.listBeneficiaries(principal.getName()).stream()
                .map(BeneficiaryResponse::fromEntity)
                .toList());
    }
}
