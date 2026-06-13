package com.netbanking.service;

import com.netbanking.entity.*;
import com.netbanking.exception.InsufficientBalanceException;
import com.netbanking.exception.InvalidTransactionException;
import com.netbanking.exception.ResourceNotFoundException;
import com.netbanking.repository.AccountRepository;
import com.netbanking.repository.PendingTransferRepository;
import com.netbanking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;
    private final RealtimeNotificationService realtimeNotificationService;
    private final BeneficiaryService beneficiaryService;
    private final OtpService otpService;
    private final PendingTransferRepository pendingTransferRepository;

    @Value("${app.transfer.otp-threshold:200000}")
    private BigDecimal otpThreshold;

    @Value("${app.transfer.admin-approval-threshold:1000000}")
    private BigDecimal adminApprovalThreshold;

    @Value("${app.transfer.daily-limit:2000000}")
    private BigDecimal dailyLimit;

    @Value("${app.transfer.suspicious-threshold:750000}")
    private BigDecimal suspiciousThreshold;

    @Transactional(rollbackFor = Exception.class)
    public TransferSubmissionResult submitTransfer(
            String sourceAccountNumber,
            String targetAccountNumber,
            BigDecimal amount,
            TransferType transferType,
            String description,
            Long beneficiaryId,
            String otpCode,
            String username,
            String ipAddress) {

        beneficiaryService.requireReadyBeneficiary(beneficiaryId, targetAccountNumber, username);

        Account sourceAccount = accountRepository.findByAccountNumber(sourceAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + sourceAccountNumber));
        if (!sourceAccount.getUser().getUsername().equals(username)) {
            throw new InvalidTransactionException("Access denied: You do not own the source account.");
        }

        enforceDailyLimit(sourceAccount, amount);

        if (amount.compareTo(suspiciousThreshold) >= 0) {
            auditLogService.log("SUSPICIOUS_TRANSFER_REVIEW", username, ipAddress,
                    String.format("Transfer of %s from %s to %s flagged for risk controls", amount, sourceAccountNumber, targetAccountNumber));
        }

        if (amount.compareTo(otpThreshold) >= 0) {
            if (otpCode == null || otpCode.isBlank()) {
                otpService.generateOtp(username, "TRANSFER");
                return TransferSubmissionResult.otpRequired();
            }
            if (!otpService.validateOtp(username, otpCode, "TRANSFER")) {
                throw new InvalidTransactionException("Invalid or expired transfer OTP.");
            }
        }

        if (amount.compareTo(adminApprovalThreshold) >= 0) {
            PendingTransfer pendingTransfer = PendingTransfer.builder()
                    .sourceAccountNumber(sourceAccountNumber)
                    .targetAccountNumber(targetAccountNumber)
                    .amount(amount)
                    .transferType(transferType)
                    .description(description)
                    .username(username)
                    .build();
            PendingTransfer saved = pendingTransferRepository.save(pendingTransfer);
            auditLogService.log("TRANSFER_PENDING_ADMIN_APPROVAL", username, ipAddress,
                    String.format("Transfer %s from %s to %s queued as approval %s", amount, sourceAccountNumber, targetAccountNumber, saved.getId()));
            return TransferSubmissionResult.pendingApproval(saved);
        }

        Transaction transaction = transferFunds(sourceAccountNumber, targetAccountNumber, amount, transferType, description, username, ipAddress);
        return TransferSubmissionResult.completed(transaction);
    }

    @Transactional(rollbackFor = Exception.class)
    public PendingTransfer approvePendingTransfer(Long pendingTransferId, String adminUsername, String ipAddress) {
        PendingTransfer pendingTransfer = pendingTransferRepository.findById(pendingTransferId)
                .orElseThrow(() -> new ResourceNotFoundException("Pending transfer not found: " + pendingTransferId));

        if (pendingTransfer.getStatus() != PendingTransferStatus.PENDING_ADMIN_APPROVAL) {
            throw new InvalidTransactionException("Pending transfer has already been decided.");
        }

        Transaction transaction = transferFunds(
                pendingTransfer.getSourceAccountNumber(),
                pendingTransfer.getTargetAccountNumber(),
                pendingTransfer.getAmount(),
                pendingTransfer.getTransferType(),
                pendingTransfer.getDescription(),
                pendingTransfer.getUsername(),
                ipAddress
        );

        pendingTransfer.setStatus(PendingTransferStatus.APPROVED);
        pendingTransfer.setDecidedAt(LocalDateTime.now());
        pendingTransfer.setDecidedBy(adminUsername);
        pendingTransfer.setTransactionReference(transaction.getTransactionReference());
        return pendingTransferRepository.save(pendingTransfer);
    }

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

        realtimeNotificationService.publishDebit(
                sourceAccount.getUser().getUsername(),
                sourceAccount.getAccountNumber(),
                sourceAccount.getBalance(),
                amount,
                transferType.name(),
                savedTransaction.getTransactionReference()
        );
        realtimeNotificationService.publishCredit(
                targetAccount.getUser().getUsername(),
                targetAccount.getAccountNumber(),
                targetAccount.getBalance(),
                amount,
                transferType.name(),
                savedTransaction.getTransactionReference()
        );

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

    @Transactional(readOnly = true)
    public List<PendingTransfer> getPendingTransfersForApproval() {
        return pendingTransferRepository.findByStatusOrderByCreatedAtAsc(PendingTransferStatus.PENDING_ADMIN_APPROVAL);
    }

    private void enforceDailyLimit(Account sourceAccount, BigDecimal amount) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        BigDecimal debitsToday = transactionRepository.sumSuccessfulDebitsSince(sourceAccount, startOfDay);
        if (debitsToday.add(amount).compareTo(dailyLimit) > 0) {
            throw new InvalidTransactionException("Daily transfer limit exceeded. Remaining limit: " + dailyLimit.subtract(debitsToday).max(BigDecimal.ZERO));
        }
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

    public record TransferSubmissionResult(
            String status,
            Transaction transaction,
            PendingTransfer pendingTransfer
    ) {
        public static TransferSubmissionResult completed(Transaction transaction) {
            return new TransferSubmissionResult("COMPLETED", transaction, null);
        }

        public static TransferSubmissionResult otpRequired() {
            return new TransferSubmissionResult("OTP_REQUIRED", null, null);
        }

        public static TransferSubmissionResult pendingApproval(PendingTransfer pendingTransfer) {
            return new TransferSubmissionResult("PENDING_ADMIN_APPROVAL", null, pendingTransfer);
        }
    }
}
