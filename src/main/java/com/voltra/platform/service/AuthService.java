package com.voltra.platform.service;

import com.voltra.platform.dto.AuthDtos.*;
import com.voltra.platform.model.Role;
import com.voltra.platform.model.User;
import com.voltra.platform.repository.UserRepository;
import com.voltra.platform.security.InputSanitizer;
import com.voltra.platform.security.JwtService;
import com.voltra.platform.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final InputSanitizer sanitizer;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenBlacklistService blacklistService,
                       InputSanitizer sanitizer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.sanitizer = sanitizer;
    }

    public AuthResponse register(RegisterRequest req) {
        String cleanEmail = req.email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new IllegalStateException("An account with this email already exists.");
        }
        if (req.role == Role.ADMIN) {
            throw new IllegalArgumentException("Self-registration for ADMIN role is prohibited.");
        }

        User user = new User();
        user.setFullName(sanitizer.clean(req.fullName));
        user.setEmail(cleanEmail);
        user.setPasswordHash(passwordEncoder.encode(req.password));
        user.setRole(req.role);
        user.setPhone(sanitizer.clean(req.phone));
        user.setVehicleModel(sanitizer.clean(req.vehicleModel));
        user.setVehiclePlate(sanitizer.clean(req.vehiclePlate));
        user.setPayoutUpi(sanitizer.clean(req.payoutUpi));
        user.setBaseAddress(sanitizer.clean(req.baseAddress));
        user.setWalletBalance(BigDecimal.ZERO);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved);
        return new AuthResponse(token, jwtService.getExpirationMs(), toDto(saved));
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!passwordEncoder.matches(req.password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, jwtService.getExpirationMs(), toDto(user));
    }

    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                Claims claims = jwtService.parseClaims(token);
                blacklistService.revokeToken(token, claims.getExpiration().getTime());
            } catch (Exception ignored) {
            }
        }
    }

    public UserProfileDto updateProfile(User currentUser, ProfileUpdateRequest req) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User profile not found."));
        user.setFullName(sanitizer.clean(req.fullName));
        if (req.phone != null) user.setPhone(sanitizer.clean(req.phone));
        if (req.vehicleModel != null) user.setVehicleModel(sanitizer.clean(req.vehicleModel));
        if (req.vehiclePlate != null) user.setVehiclePlate(sanitizer.clean(req.vehiclePlate));
        if (req.payoutUpi != null) user.setPayoutUpi(sanitizer.clean(req.payoutUpi));
        if (req.baseAddress != null) user.setBaseAddress(sanitizer.clean(req.baseAddress));
        return toDto(userRepository.save(user));
    }

    public UserProfileDto toDto(User u) {
        UserProfileDto dto = new UserProfileDto();
        dto.id = u.getId();
        dto.fullName = u.getFullName();
        dto.email = u.getEmail();
        dto.role = u.getRole();
        dto.phone = u.getPhone();
        dto.vehicleModel = u.getVehicleModel();
        dto.vehiclePlate = u.getVehiclePlate();
        dto.payoutUpi = u.getPayoutUpi();
        dto.baseAddress = u.getBaseAddress();
        dto.walletBalance = u.getWalletBalance();
        return dto;
    }
}
