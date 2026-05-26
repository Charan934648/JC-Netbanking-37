package com.netbanking.service;

import com.netbanking.entity.*;
import com.netbanking.exception.InvalidTransactionException;
import com.netbanking.exception.ResourceNotFoundException;
import com.netbanking.repository.AccountRepository;
import com.netbanking.repository.TransactionRepository;
import com.netbanking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;
    private final AccountNumberGenerator accountNumberGenerator;

    @Transactional(readOnly = true)
    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return accountRepository.findByUser(user);
    }

    @Transactional
    public Account createAccountForUser(String username, String accountType, BigDecimal initialBalance, String ipAddress) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!accountType.equalsIgnoreCase("SAVINGS") && !accountType.equalsIgnoreCase("CURRENT")) {
            throw new InvalidTransactionException("Invalid account type. Must be SAVINGS or CURRENT.");
        }

        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransactionException("Initial balance cannot be negative.");
        }

        Account account = Account.builder()
                .user(user)
                .accountNumber(accountNumberGenerator.generateUniqueAccountNumber())
                .accountType(accountType.toUpperCase())
                .balance(initialBalance)
                .build();

        Account savedAccount = accountRepository.save(account);

        // Record a transaction for the initial deposit if any
        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            Transaction transaction = Transaction.builder()
                    .sourceAccount(null)
                    .targetAccount(savedAccount)
                    .amount(initialBalance)
                    .transferType(TransferType.DEPOSIT)
                    .status(TransactionStatus.SUCCESS)
                    .description("Initial Deposit")
                    .build();
            transactionRepository.save(transaction);
        }

        auditLogService.log("ACCOUNT_CREATION", user.getUsername(), ipAddress,
                String.format("Created new %s account %s with initial balance %s",
                        accountType, savedAccount.getAccountNumber(), initialBalance));

        return savedAccount;
    }

    @Transactional
    public Account deposit(String accountNumber, BigDecimal amount, String username, String ipAddress) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be positive.");
        }

        // Lock the account row to update balance safely
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        if (!account.getUser().getUsername().equals(username)) {
            auditLogService.log("UNAUTHORIZED_DEPOSIT_ATTEMPT", username, ipAddress,
                    String.format("User %s tried to deposit into account %s which they do not own",
                            username, accountNumber));
            throw new InvalidTransactionException("Access denied: You do not own this account.");
        }

        account.setBalance(account.getBalance().add(amount));
        Account updatedAccount = accountRepository.save(account);

        // Record deposit transaction
        Transaction transaction = Transaction.builder()
                .sourceAccount(null)
                .targetAccount(updatedAccount)
                .amount(amount)
                .transferType(TransferType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .description("Cash Deposit")
                .build();
        transactionRepository.save(transaction);

        auditLogService.log("CASH_DEPOSIT_SUCCESS", account.getUser().getUsername(), ipAddress,
                String.format("Deposited %s into account %s. New Balance: %s",
                        amount, accountNumber, updatedAccount.getBalance()));

        return updatedAccount;
    }
}
