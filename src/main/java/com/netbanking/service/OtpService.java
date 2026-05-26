package com.netbanking.service;

import com.netbanking.entity.OtpVerification;
import com.netbanking.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpVerificationRepository otpVerificationRepository;

    @Transactional
    public String generateOtp(String username) {
        int number = 100000 + SECURE_RANDOM.nextInt(900000); // 6-digit number
        String otpCode = String.valueOf(number);

        OtpVerification otpVerification = OtpVerification.builder()
                .username(username)
                .otpCode(otpCode)
                .expiryTime(LocalDateTime.now().plusMinutes(5)) // Expiration time: 5 minutes
                .verified(false)
                .build();

        otpVerificationRepository.save(otpVerification);

        log.info("OTP generated for user '{}'. Deliver this code through the configured secure channel.", username);

        return otpCode;
    }

    @Transactional
    public boolean validateOtp(String username, String otpCode) {
        Optional<OtpVerification> verificationOpt = otpVerificationRepository
                .findTopByUsernameAndOtpCodeAndVerifiedOrderByExpiryTimeDesc(username, otpCode, false);

        if (verificationOpt.isPresent()) {
            OtpVerification verification = verificationOpt.get();
            if (!verification.isExpired()) {
                verification.setVerified(true);
                otpVerificationRepository.save(verification);
                log.info("OTP verification successful for user '{}'", username);
                return true;
            } else {
                log.warn("Expired OTP submitted for user '{}'", username);
            }
        } else {
            log.warn("Invalid OTP submitted for user '{}'", username);
        }
        return false;
    }
}
