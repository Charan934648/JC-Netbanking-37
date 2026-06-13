package com.netbanking.service;

import com.netbanking.entity.User;
import com.netbanking.repository.UserRepository;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpOtpNotificationService {

    private final UserRepository userRepository;

    @Value("${app.otp.email.from:no-reply@jcbank.local}")
    private String fromAddress;

    @Value("${app.otp.smtp.host:}")
    private String smtpHost;

    @Value("${app.otp.smtp.port:587}")
    private int smtpPort;

    @Value("${app.otp.smtp.username:}")
    private String smtpUsername;

    @Value("${app.otp.smtp.password:}")
    private String smtpPassword;

    @Value("${app.otp.smtp.starttls:true}")
    private boolean startTls;

    public boolean isConfigured() {
        return smtpHost != null && !smtpHost.isBlank();
    }

    public void sendOtp(String username, String purpose, String otpCode) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found for OTP delivery: " + username));

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", String.valueOf(smtpUsername != null && !smtpUsername.isBlank()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", String.valueOf(smtpPort));

        Session session = Session.getInstance(properties, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (smtpUsername == null || smtpUsername.isBlank()) {
                    return null;
                }
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.getEmail()));
            message.setSubject("JC Bank " + purpose + " OTP");
            message.setText("Your JC Bank " + purpose + " OTP is " + otpCode + ". It expires in 5 minutes.");
            Transport.send(message);
            log.info("Delivered {} OTP to registered email for user '{}'", purpose, username);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to send OTP email", ex);
        }
    }
}
