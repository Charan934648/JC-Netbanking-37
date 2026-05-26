package com.netbanking.controller;

import com.netbanking.dto.*;
import com.netbanking.entity.Role;
import com.netbanking.entity.User;
import com.netbanking.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest request) {

        User user = userService.registerUser(
                registerRequest.username(),
                registerRequest.password(),
                registerRequest.email(),
                Role.ROLE_USER,
                request.getRemoteAddr()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login/initiate")
    public ResponseEntity<Map<String, String>> initiateLogin(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        userService.initiateLogin(loginRequest.username(), loginRequest.password(), request.getRemoteAddr());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Credentials verified. Please verify the 6-digit OTP code from the configured secure channel.");
        response.put("username", loginRequest.username());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/verify")
    public ResponseEntity<LoginResponse> verifyLogin(
            @Valid @RequestBody VerifyOtpRequest verifyOtpRequest,
            HttpServletRequest request) {

        String token = userService.verifyLoginOtp(
                verifyOtpRequest.username(),
                verifyOtpRequest.otpCode(),
                request.getRemoteAddr()
        );

        User user = userService.getUserByUsername(verifyOtpRequest.username());
        LoginResponse loginResponse = new LoginResponse(token, user.getUsername(), user.getRole().name());

        return ResponseEntity.ok(loginResponse);
    }
}
