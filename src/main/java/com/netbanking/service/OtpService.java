package com.netbanking.service;

import com.netbanking.entity.OtpVerification;
import com.netbanking.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final OtpNotificationService otpNotificationService;

    @Value("${app.otp.expiry-minutes:5}")
    private long expiryMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Transactional
    public String generateOtp(String username) {
        return generateOtp(username, "LOGIN", OtpDeliveryChannel.EMAIL);
    }

    @Transactional
    public String generateOtp(String username, String purpose) {
        return generateOtp(username, purpose, OtpDeliveryChannel.EMAIL);
    }

    @Transactional
    public String generateOtp(String username, String purpose, OtpDeliveryChannel preferredChannel) {
        int number = 100000 + SECURE_RANDOM.nextInt(900000); // 6-digit number
        String otpCode = String.valueOf(number);

        OtpVerification otpVerification = OtpVerification.builder()
                .username(username)
                .otpCode(otpCode)
                .purpose(purpose)
                .expiryTime(LocalDateTime.now().plusMinutes(expiryMinutes))
                .verified(false)
                .build();

        otpVerificationRepository.save(otpVerification);

        otpNotificationService.sendOtp(username, purpose, otpCode, preferredChannel);

        return otpCode;
    }

    @Transactional
    public boolean validateOtp(String username, String otpCode) {
        return validateOtp(username, otpCode, "LOGIN");
    }

    @Transactional
    public boolean validateOtp(String username, String otpCode, String purpose) {
        Optional<OtpVerification> verificationOpt = otpVerificationRepository
                .findTopByUsernameAndPurposeAndVerifiedOrderByExpiryTimeDesc(username, purpose, false);

        if (verificationOpt.isPresent()) {
            OtpVerification verification = verificationOpt.get();
            verification.setAttempts(verification.getAttempts() + 1);
            otpVerificationRepository.save(verification);

            if (verification.getAttempts() > maxAttempts) {
                log.warn("OTP attempt limit exceeded for user '{}' and purpose '{}'", username, purpose);
                return false;
            }

            if (!verification.isExpired() && verification.getOtpCode().equals(otpCode)) {
                verification.setVerified(true);
                otpVerificationRepository.save(verification);
                log.info("OTP verification successful for user '{}' and purpose '{}'", username, purpose);
                return true;
            } else {
                log.warn("Invalid or expired OTP submitted for user '{}' and purpose '{}'", username, purpose);
            }
        } else {
            log.warn("Invalid OTP submitted for user '{}' and purpose '{}'", username, purpose);
        }
        return false;
    }
}
