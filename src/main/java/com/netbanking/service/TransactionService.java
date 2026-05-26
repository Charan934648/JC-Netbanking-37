package com.netbanking.service;

import com.netbanking.entity.*;
import com.netbanking.exception.InsufficientBalanceException;
import com.netbanking.exception.InvalidTransactionException;
import com.netbanking.exception.ResourceNotFoundException;
import com.netbanking.repository.AccountRepository;
import com.netbanking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;

    @Transactional(rollbackFor = Exception.class)
    public Transaction transferFunds(
            String sourceAccountNumber,
            String targetAccountNumber,
            BigDecimal amount,
            TransferType transferType,
            String description,
            String username,
            String ipAddress) {

        if (sourceAccountNumber.equals(targetAccountNumber)) {
            throw new InvalidTransactionException("Source and target accounts must be different.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be positive.");
        }

        // Validate transfer limits based on type
        validateLimits(transferType, amount);

        // Lock rows in a consistent lexicographical order of account numbers to prevent deadlocks
        Account sourceAccount;
        Account targetAccount;
        if (sourceAccountNumber.compareTo(targetAccountNumber) < 0) {
            sourceAccount = accountRepository.findByAccountNumberWithLock(sourceAccountNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + sourceAccountNumber));
            targetAccount = accountRepository.findByAccountNumberWithLock(targetAccountNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Target account not found: " + targetAccountNumber));
        } else {
            targetAccount = accountRepository.findByAccountNumberWithLock(targetAccountNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Target account not found: " + targetAccountNumber));
            sourceAccount = accountRepository.findByAccountNumberWithLock(sourceAccountNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + sourceAccountNumber));
        }

        // Security check: User must own the source account
        if (!sourceAccount.getUser().getUsername().equals(username)) {
            auditLogService.log("UNAUTHORIZED_TRANSFER_ATTEMPT", username, ipAddress,
                    String.format("User %s tried to transfer from account %s which they do not own",
                            username, sourceAccountNumber));
            throw new InvalidTransactionException("Access denied: You do not own the source account.");
        }

        // Balance check
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            auditLogService.log("TRANSFER_FAILED", username, ipAddress,
                    String.format("Insufficient funds in account %s. Requested: %s, Available: %s",
                            sourceAccountNumber, amount, sourceAccount.getBalance()));
            throw new InsufficientBalanceException("Insufficient balance in source account.");
        }

        // Perform balance updates
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        targetAccount.setBalance(targetAccount.getBalance().add(amount));

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        // Record the transaction
        Transaction transaction = Transaction.builder()
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(amount)
                .transferType(transferType)
                .status(TransactionStatus.SUCCESS)
                .description(description)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        auditLogService.log("TRANSFER_SUCCESS", username, ipAddress,
                String.format("Transferred %s via %s from %s to %s. Ref: %s",
                        amount, transferType, sourceAccountNumber, targetAccountNumber, savedTransaction.getTransactionReference()));

        return savedTransaction;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getStatement(String accountNumber, String username) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        // Security check: Users can only see statements for their own accounts
        if (!account.getUser().getUsername().equals(username)) {
            throw new InvalidTransactionException("Access denied: You do not own this account.");
        }

        return transactionRepository.findBySourceAccountOrTargetAccountOrderByTimestampDesc(account, account);
    }

    private void validateLimits(TransferType type, BigDecimal amount) {
        switch (type) {
            case IMPS:
                BigDecimal maxImps = BigDecimal.valueOf(5000000); // 5,000,000
                if (amount.compareTo(maxImps) > 0) {
                    throw new InvalidTransactionException("IMPS transfer limit exceeded. Max amount: " + maxImps);
                }
                break;
            case NEFT:
                BigDecimal maxNeft = BigDecimal.valueOf(10000000); // 10,000,000
                if (amount.compareTo(maxNeft) > 0) {
                    throw new InvalidTransactionException("NEFT transfer limit exceeded. Max amount: " + maxNeft);
                }
                break;
            case RTGS:
                BigDecimal minRtgs = BigDecimal.valueOf(200000); // 200,000
                BigDecimal maxRtgs = BigDecimal.valueOf(50000000); // 50,000,000
                if (amount.compareTo(minRtgs) < 0) {
                    throw new InvalidTransactionException("RTGS minimum transfer amount is: " + minRtgs);
                }
                if (amount.compareTo(maxRtgs) > 0) {
                    throw new InvalidTransactionException("RTGS transfer limit exceeded. Max amount: " + maxRtgs);
                }
                break;
            default:
                throw new InvalidTransactionException("Invalid transfer type selected.");
        }
    }
}
