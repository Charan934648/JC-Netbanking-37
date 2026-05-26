package com.netbanking.service;

import com.netbanking.config.JwtTokenProvider;
import com.netbanking.entity.Account;
import com.netbanking.entity.Role;
import com.netbanking.entity.User;
import com.netbanking.exception.InvalidTransactionException;
import com.netbanking.exception.ResourceNotFoundException;
import com.netbanking.repository.AccountRepository;
import com.netbanking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;
    private final AccountNumberGenerator accountNumberGenerator;

    @Transactional
    public User registerUser(String username, String password, String email, Role role, String ipAddress) {
        if (userRepository.existsByUsername(username)) {
            throw new InvalidTransactionException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new InvalidTransactionException("Email is already registered");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .role(role)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // Auto-create a default SAVINGS account for the newly registered user
        Account account = Account.builder()
                .user(savedUser)
                .accountNumber(accountNumberGenerator.generateUniqueAccountNumber())
                .accountType("SAVINGS")
                .balance(BigDecimal.valueOf(10000.00)) // Give 10,000 initial bonus balance for easy testing
                .build();
        accountRepository.save(account);

        auditLogService.log("USER_REGISTRATION", "SYSTEM", ipAddress, 
                String.format("User %s registered successfully with auto-created SAVINGS account %s", 
                        username, account.getAccountNumber()));

        return savedUser;
    }

    @Transactional(readOnly = true)
    public boolean initiateLogin(String username, String password, String ipAddress) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            auditLogService.log("LOGIN_FAILED", username, ipAddress, "Invalid credentials submitted");
            throw new InvalidTransactionException("Invalid username or password");
        }

        User user = userOpt.get();
        if (!user.isEnabled()) {
            auditLogService.log("LOGIN_FAILED", username, ipAddress, "Disabled user account login attempt");
            throw new InvalidTransactionException("User account is disabled");
        }

        // Trigger OTP generation and console dispatch
        otpService.generateOtp(username);
        
        auditLogService.log("LOGIN_INITIATED", username, ipAddress, "Credentials verified, 2FA OTP triggered");
        return true;
    }

    @Transactional
    public String verifyLoginOtp(String username, String otpCode, String ipAddress) {
        boolean isValid = otpService.validateOtp(username, otpCode);
        
        if (!isValid) {
            auditLogService.log("2FA_VERIFICATION_FAILED", username, ipAddress, "Incorrect or expired OTP code");
            throw new InvalidTransactionException("Invalid or expired OTP");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());

        auditLogService.log("LOGIN_SUCCESS", username, ipAddress, "Successful 2FA verification, JWT token issued");
        return token;
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
