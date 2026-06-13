package com.netbanking.service;

import com.netbanking.config.JwtTokenProvider;
import com.netbanking.entity.Role;
import com.netbanking.entity.User;
import com.netbanking.repository.AccountRepository;
import com.netbanking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("user")
                .password("encoded")
                .email("user@jcbank.com")
                .phoneNumber("9000000002")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();
    }

    @Test
    void initiateLoginAcceptsUsername() {
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("user123", "encoded")).thenReturn(true);
        when(otpService.generateOtp("user", "LOGIN", OtpDeliveryChannel.EMAIL)).thenReturn("123456");

        String otp = userService.initiateLogin("user", "user123", "127.0.0.1");

        assertEquals("123456", otp);
        verify(userRepository).findByUsername("user");
    }

    @Test
    void initiateLoginAcceptsEmail() {
        when(userRepository.findByEmail("user@jcbank.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("user123", "encoded")).thenReturn(true);
        when(otpService.generateOtp("user", "LOGIN", OtpDeliveryChannel.EMAIL)).thenReturn("123456");

        String otp = userService.initiateLogin("user@jcbank.com", "user123", "127.0.0.1");

        assertEquals("123456", otp);
        verify(userRepository).findByEmail("user@jcbank.com");
    }

    @Test
    void initiateLoginAcceptsPhoneNumber() {
        when(userRepository.findByPhoneNumber("9000000002")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("user123", "encoded")).thenReturn(true);
        when(otpService.generateOtp("user", "LOGIN", OtpDeliveryChannel.SMS)).thenReturn("123456");

        String otp = userService.initiateLogin("9000000002", "user123", "127.0.0.1");

        assertEquals("123456", otp);
        verify(userRepository).findByPhoneNumber("9000000002");
    }
}
