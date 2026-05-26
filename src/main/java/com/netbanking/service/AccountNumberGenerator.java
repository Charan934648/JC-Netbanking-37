package com.netbanking.service;

import com.netbanking.exception.InvalidTransactionException;
import com.netbanking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private static final int MAX_ATTEMPTS = 20;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;

    public String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String accountNumber = generateAccountNumber();
            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }
        throw new InvalidTransactionException("Could not generate a unique account number. Please retry.");
    }

    private String generateAccountNumber() {
        StringBuilder sb = new StringBuilder();
        sb.append(SECURE_RANDOM.nextInt(9) + 1);
        for (int i = 0; i < 9; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
