package com.netbanking.service;

import com.netbanking.entity.*;
import com.netbanking.exception.InsufficientBalanceException;
import com.netbanking.exception.InvalidTransactionException;
import com.netbanking.exception.ResourceNotFoundException;
import com.netbanking.repository.AccountRepository;
import com.netbanking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BillPaymentService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;
    private final RealtimeNotificationService realtimeNotificationService;

    @Transactional(rollbackFor = Exception.class)
    public Transaction payBill(
            String accountNumber,
            String billerName,
            String consumerNumber,
            BigDecimal amount,
            String username,
            String ipAddress) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Bill payment amount must be positive.");
        }

        if (billerName == null || billerName.trim().isEmpty()) {
            throw new InvalidTransactionException("Biller name is required.");
        }

        if (consumerNumber == null || consumerNumber.trim().isEmpty()) {
            throw new InvalidTransactionException("Consumer/Customer reference number is required.");
        }

        // Lock row to update balance safely
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        // Ownership validation
        if (!account.getUser().getUsername().equals(username)) {
            auditLogService.log("UNAUTHORIZED_BILLPAY_ATTEMPT", username, ipAddress,
                    String.format("User %s tried to pay bill from account %s which they do not own",
                            username, accountNumber));
            throw new InvalidTransactionException("Access denied: You do not own this account.");
        }

        // Balance validation
        if (account.getBalance().compareTo(amount) < 0) {
            auditLogService.log("BILLPAY_FAILED", username, ipAddress,
                    String.format("Insufficient funds for bill payment from %s. Amount: %s, Balance: %s",
                            accountNumber, amount, account.getBalance()));
            throw new InsufficientBalanceException("Insufficient balance for bill payment.");
        }

        // Subtract balance
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        // Record external bill payment transaction
        Transaction transaction = Transaction.builder()
                .sourceAccount(account)
                .targetAccount(null) // null targets represent external withdrawals/payments
                .amount(amount)
                .transferType(TransferType.BILL_PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .description(String.format("Bill Payment to %s (%s)", billerName, consumerNumber))
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        auditLogService.log("BILLPAY_SUCCESS", username, ipAddress,
                String.format("Paid bill of %s to %s from %s. Ref: %s",
                        amount, billerName, accountNumber, savedTransaction.getTransactionReference()));

        realtimeNotificationService.publishDebit(
                account.getUser().getUsername(),
                account.getAccountNumber(),
                account.getBalance(),
                amount,
                TransferType.BILL_PAYMENT.name(),
                savedTransaction.getTransactionReference()
        );

        return savedTransaction;
    }
}
