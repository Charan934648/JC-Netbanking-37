package com.netbanking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConsoleOtpNotificationService implements OtpNotificationService {

    private final SmtpOtpNotificationService smtpOtpNotificationService;
    private final SmsOtpNotificationService smsOtpNotificationService;

    @Value("${app.otp.delivery-provider:console}")
    private String deliveryProvider;

    public ConsoleOtpNotificationService(
            SmtpOtpNotificationService smtpOtpNotificationService,
            SmsOtpNotificationService smsOtpNotificationService) {
        this.smtpOtpNotificationService = smtpOtpNotificationService;
        this.smsOtpNotificationService = smsOtpNotificationService;
    }

    @Override
    public void sendOtp(String username, String purpose, String otpCode, OtpDeliveryChannel preferredChannel) {
        if (preferredChannel == OtpDeliveryChannel.SMS && trySms(username, purpose, otpCode)) {
            return;
        }
        if (preferredChannel == OtpDeliveryChannel.EMAIL && tryEmail(username, purpose, otpCode)) {
            return;
        }
        if ("sms".equalsIgnoreCase(deliveryProvider) && trySms(username, purpose, otpCode)) {
            return;
        }
        if ("email".equalsIgnoreCase(deliveryProvider) && tryEmail(username, purpose, otpCode)) {
            return;
        }

        log.info("JC Bank {} OTP for user '{}': {}", purpose, username, otpCode);
    }

    private boolean tryEmail(String username, String purpose, String otpCode) {
        if (!smtpOtpNotificationService.isConfigured()) {
            return false;
        }
        try {
            smtpOtpNotificationService.sendOtp(username, purpose, otpCode);
            return true;
        } catch (RuntimeException ex) {
            log.warn("Email OTP delivery failed for user '{}'. Falling back to another channel.", username, ex);
            return false;
        }
    }

    private boolean trySms(String username, String purpose, String otpCode) {
        if (!smsOtpNotificationService.isConfigured()) {
            return false;
        }
        try {
            smsOtpNotificationService.sendOtp(username, purpose, otpCode);
            return true;
        } catch (RuntimeException ex) {
            log.warn("SMS OTP delivery failed for user '{}'. Falling back to another channel.", username, ex);
            return false;
        }
    }
}
