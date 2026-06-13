package com.netbanking.service;

import com.netbanking.entity.User;
import com.netbanking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsOtpNotificationService {

    private final UserRepository userRepository;

    @Value("${app.otp.sms.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.otp.sms.twilio.auth-token:}")
    private String authToken;

    @Value("${app.otp.sms.twilio.from-number:}")
    private String fromNumber;

    public boolean isConfigured() {
        return accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && fromNumber != null && !fromNumber.isBlank();
    }

    public void sendOtp(String username, String purpose, String otpCode) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found for OTP delivery: " + username));
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            throw new IllegalStateException("User does not have a registered phone number.");
        }

        String toNumber = normalizePhone(user.getPhoneNumber());
        String message = "Your JC Bank " + purpose + " OTP is " + otpCode + ". It expires in 5 minutes.";
        String body = "To=" + encode(toNumber)
                + "&From=" + encode(fromNumber)
                + "&Body=" + encode(message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
                .header("Authorization", "Basic " + Base64.getEncoder()
                        .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("SMS provider returned " + response.statusCode() + ": " + response.body());
            }
            log.info("Delivered {} OTP to registered phone for user '{}'", purpose, username);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to send OTP SMS", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while sending OTP SMS", ex);
        }
    }

    private String normalizePhone(String phoneNumber) {
        String cleaned = phoneNumber.trim();
        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        return "+" + cleaned.replaceAll("[^0-9]", "");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
