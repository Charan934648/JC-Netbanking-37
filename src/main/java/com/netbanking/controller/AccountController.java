package com.netbanking.controller;

import com.netbanking.dto.AccountResponse;
import com.netbanking.dto.CreateAccountRequest;
import com.netbanking.dto.DepositRequest;
import com.netbanking.dto.TransactionResponse;
import com.netbanking.entity.Account;
import com.netbanking.entity.Transaction;
import com.netbanking.service.AccountService;
import com.netbanking.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest createAccountRequest,
            Principal principal,
            HttpServletRequest request) {

        Account account = accountService.createAccountForUser(
                principal.getName(),
                createAccountRequest.accountType(),
                createAccountRequest.initialBalance(),
                request.getRemoteAddr()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.fromEntity(account));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts(Principal principal) {
        List<Account> accounts = accountService.getAccountsByUsername(principal.getName());
        List<AccountResponse> responses = accounts.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountDetails(
            @PathVariable String accountNumber,
            Principal principal) {
        
        Account account = accountService.getAccountByNumber(accountNumber);
        
        // Security check: User can only fetch details of their own accounts
        if (!account.getUser().getUsername().equals(principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(AccountResponse.fromEntity(account));
    }

    @PostMapping("/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @Valid @RequestBody DepositRequest depositRequest,
            Principal principal,
            HttpServletRequest request) {

        Account account = accountService.deposit(
                depositRequest.accountNumber(),
                depositRequest.amount(),
                principal.getName(),
                request.getRemoteAddr()
        );

        return ResponseEntity.ok(AccountResponse.fromEntity(account));
    }

    @GetMapping("/{accountNumber}/statement")
    public ResponseEntity<List<TransactionResponse>> getStatement(
            @PathVariable String accountNumber,
            Principal principal) {

        List<Transaction> transactions = transactionService.getStatement(accountNumber, principal.getName());
        List<TransactionResponse> responses = transactions.stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
