package com.netbanking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConsoleOtpNotificationService implements OtpNotificationService {

    private final SmtpOtpNotificationService smtpOtpNotificationService;

    @Value("${app.otp.delivery-provider:console}")
    private String deliveryProvider;

    public ConsoleOtpNotificationService(SmtpOtpNotificationService smtpOtpNotificationService) {
        this.smtpOtpNotificationService = smtpOtpNotificationService;
    }

    @Override
    public void sendOtp(String username, String purpose, String otpCode) {
        if ("email".equalsIgnoreCase(deliveryProvider) && smtpOtpNotificationService.isConfigured()) {
            smtpOtpNotificationService.sendOtp(username, purpose, otpCode);
            return;
        }

        log.info("JC Bank {} OTP for user '{}': {}", purpose, username, otpCode);
    }
}
