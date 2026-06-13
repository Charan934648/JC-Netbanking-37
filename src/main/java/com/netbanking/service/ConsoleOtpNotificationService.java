package com.netbanking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConsoleOtpNotificationService implements OtpNotificationService {

    @Override
    public void sendOtp(String username, String purpose, String otpCode) {
        log.info("JC Bank {} OTP for user '{}': {}", purpose, username, otpCode);
    }
}
