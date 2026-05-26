package com.netbanking.config;

import com.netbanking.entity.Account;
import com.netbanking.entity.Role;
import com.netbanking.entity.User;
import com.netbanking.repository.AccountRepository;
import com.netbanking.repository.UserRepository;
import com.netbanking.service.AccountNumberGenerator;
import com.netbanking.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final AccountNumberGenerator accountNumberGenerator;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("No users found in database. Initializing default NetBanking users...");

            // 1. Seed Admin
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@netbanking.com")
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            auditLogService.log("ADMIN_SEEDED", "SYSTEM", "127.0.0.1", "Default Admin user seeded successfully");

            // 2. Seed Standard User 1
            User user1 = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .email("user@netbanking.com")
                    .role(Role.ROLE_USER)
                    .enabled(true)
                    .build();
            userRepository.save(user1);

            Account account1 = Account.builder()
                    .user(user1)
                    .accountNumber(accountNumberGenerator.generateUniqueAccountNumber())
                    .accountType("SAVINGS")
                    .balance(BigDecimal.valueOf(10000.00)) // $10,000 balance
                    .build();
            accountRepository.save(account1);
            auditLogService.log("USER_SEEDED", "SYSTEM", "127.0.0.1", 
                    "Default User 'user' seeded with SAVINGS account " + account1.getAccountNumber());

            // 3. Seed Standard User 2 (for transfers)
            User user2 = User.builder()
                    .username("receiver")
                    .password(passwordEncoder.encode("user123"))
                    .email("receiver@netbanking.com")
                    .role(Role.ROLE_USER)
                    .enabled(true)
                    .build();
            userRepository.save(user2);

            Account account2 = Account.builder()
                    .user(user2)
                    .accountNumber(accountNumberGenerator.generateUniqueAccountNumber())
                    .accountType("SAVINGS")
                    .balance(BigDecimal.valueOf(5000.00)) // $5,000 balance
                    .build();
            accountRepository.save(account2);
            auditLogService.log("USER_SEEDED", "SYSTEM", "127.0.0.1", 
                    "Default User 'receiver' seeded with SAVINGS account " + account2.getAccountNumber());

            log.info("=========================================================");
            log.info("NETBANKING DB SEEDING COMPLETED:");
            log.info("  1. Admin Account: Username: admin / Password: admin123");
            log.info("  2. User Account:  Username: user  / Password: user123");
            log.info("     Account Number: {} | Initial Balance: 10000.00", account1.getAccountNumber());
            log.info("  3. User Account:  Username: receiver / Password: user123");
            log.info("     Account Number: {} | Initial Balance: 5000.00", account2.getAccountNumber());
            log.info("=========================================================");
        } else {
            log.info("Database already contains users. Skipping seeding initialization.");
            // Print account list in logs for easy testing reference anyway
            List<Account> accounts = accountRepository.findAll();
            log.info("=========================================================");
            log.info("EXISTING ACCOUNTS IN SYSTEM:");
            accounts.forEach(acc -> log.info("  Username: {} | Account Number: {} | Balance: {} | Type: {}", 
                    acc.getUser().getUsername(), acc.getAccountNumber(), acc.getBalance(), acc.getAccountType()));
            log.info("=========================================================");
        }
    }
}
