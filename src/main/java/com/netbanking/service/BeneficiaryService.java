package com.netbanking.service;

import com.netbanking.entity.*;
import com.netbanking.exception.InvalidTransactionException;
import com.netbanking.exception.ResourceNotFoundException;
import com.netbanking.repository.AccountRepository;
import com.netbanking.repository.BeneficiaryRepository;
import com.netbanking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    @Value("${app.beneficiary.approval-delay-minutes:0}")
    private long approvalDelayMinutes;

    @Transactional
    public Beneficiary createBeneficiary(String username, String nickname, String bankName, String ifscCode, String accountNumber, String ipAddress) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (beneficiaryRepository.existsByUserAndAccountNumber(user, accountNumber)) {
            throw new InvalidTransactionException("Beneficiary already exists for this account.");
        }

        if (accountRepository.findByAccountNumber(accountNumber).isEmpty()) {
            throw new ResourceNotFoundException("Beneficiary account could not be verified: " + accountNumber);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime availableAt = now.plusMinutes(approvalDelayMinutes);
        BeneficiaryStatus status = approvalDelayMinutes <= 0 ? BeneficiaryStatus.ACTIVE : BeneficiaryStatus.PENDING;

        Beneficiary beneficiary = Beneficiary.builder()
                .user(user)
                .nickname(nickname.trim())
                .bankName(bankName.trim())
                .ifscCode(ifscCode.trim().toUpperCase())
                .accountNumber(accountNumber)
                .status(status)
                .availableAt(availableAt)
                .verifiedAt(now)
                .build();

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        auditLogService.log("BENEFICIARY_ADDED", username, ipAddress,
                String.format("Added beneficiary %s for account %s with status %s", saved.getNickname(), saved.getAccountNumber(), saved.getStatus()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Beneficiary> listBeneficiaries(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return beneficiaryRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Beneficiary requireReadyBeneficiary(Long beneficiaryId, String targetAccountNumber, String username) {
        if (beneficiaryId == null) {
            throw new InvalidTransactionException("Select an approved beneficiary before transferring funds.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUser(beneficiaryId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found: " + beneficiaryId));

        if (!beneficiary.getAccountNumber().equals(targetAccountNumber)) {
            throw new InvalidTransactionException("Transfer target does not match selected beneficiary.");
        }
        if (!beneficiary.isReadyForTransfers()) {
            throw new InvalidTransactionException("Beneficiary is verified but not active for transfers yet.");
        }

        return beneficiary;
    }
}
