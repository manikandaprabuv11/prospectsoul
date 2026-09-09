package com.vyoog.prospectsoul_backend.auth.controller;

import com.vyoog.prospectsoul_backend.auth.dto.request.ForgotPasswordRequest;
import com.vyoog.prospectsoul_backend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.triggerPasswordReset(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "If an account with that email exists, a password reset link has been sent."));
    }
}
