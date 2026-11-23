package com.voltra.platform.controller;

import com.voltra.platform.dto.AuthDtos.*;
import com.voltra.platform.model.User;
import com.voltra.platform.security.RateLimiterService;
import com.voltra.platform.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final RateLimiterService rateLimiter;

    public AuthController(AuthService authService, RateLimiterService rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest httpReq) {
        if (!rateLimiter.allowAuthAttempt(extractIp(httpReq))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("status", 429, "message", "Rate limit exceeded (5 attempts/min). Please try again later."));
        }
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
        if (!rateLimiter.allowAuthAttempt(extractIp(httpReq))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("status", 429, "message", "Rate limit exceeded (5 logins/min). Please wait 1 minute."));
        }
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(Map.of("message", "Logged out securely and session token revoked."));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(authService.toDto(currentUser));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(@AuthenticationPrincipal User currentUser,
                                                        @Valid @RequestBody ProfileUpdateRequest req) {
        return ResponseEntity.ok(authService.updateProfile(currentUser, req));
    }

    private String extractIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
