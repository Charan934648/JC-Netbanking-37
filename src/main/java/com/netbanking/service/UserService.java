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
    public User registerUser(String username, String password, String email, String phoneNumber, Role role, String ipAddress) {
        if (userRepository.existsByUsername(username)) {
            throw new InvalidTransactionException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new InvalidTransactionException("Email is already registered");
        }
        String normalizedPhone = normalizePhone(phoneNumber);
        if (normalizedPhone != null && userRepository.existsByPhoneNumber(normalizedPhone)) {
            throw new InvalidTransactionException("Phone number is already registered");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .phoneNumber(normalizedPhone)
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

    @Transactional
    public String initiateLogin(String username, String password, String ipAddress) {
        Optional<User> userOpt = findByLoginIdentifier(username);
        
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
        String otpCode = otpService.generateOtp(user.getUsername(), "LOGIN");
        
        auditLogService.log("LOGIN_INITIATED", user.getUsername(), ipAddress, "Credentials verified, 2FA OTP triggered");
        return otpCode;
    }

    @Transactional
    public String verifyLoginOtp(String username, String otpCode, String ipAddress) {
        User user = findByLoginIdentifier(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        boolean isValid = otpService.validateOtp(user.getUsername(), otpCode, "LOGIN");
        
        if (!isValid) {
            auditLogService.log("2FA_VERIFICATION_FAILED", user.getUsername(), ipAddress, "Incorrect or expired OTP code");
            throw new InvalidTransactionException("Invalid or expired OTP");
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());

        auditLogService.log("LOGIN_SUCCESS", user.getUsername(), ipAddress, "Successful 2FA verification, JWT token issued");
        return token;
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return findByLoginIdentifier(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Optional<User> findByLoginIdentifier(String identifier) {
        String normalized = identifier == null ? "" : identifier.trim();
        if (normalized.contains("@")) {
            return userRepository.findByEmail(normalized);
        }
        String digitsOnly = normalized.replaceAll("[^0-9]", "");
        if (digitsOnly.matches("^[0-9]{10,15}$")) {
            return userRepository.findByPhoneNumber(digitsOnly);
        }
        return userRepository.findByUsername(normalized);
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        return phoneNumber.replaceAll("[^0-9]", "");
    }
}
