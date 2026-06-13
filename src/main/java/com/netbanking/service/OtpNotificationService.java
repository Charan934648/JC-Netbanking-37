package com.netbanking.service;

public interface OtpNotificationService {
    void sendOtp(String username, String purpose, String otpCode, OtpDeliveryChannel preferredChannel);
}
