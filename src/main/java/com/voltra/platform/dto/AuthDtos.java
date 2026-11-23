package com.voltra.platform.dto;

import com.voltra.platform.model.Role;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class AuthDtos {
    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100)
        public String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        @Size(max = 120)
        public String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain letters and numbers")
        public String password;

        @NotNull(message = "Role is required")
        public Role role;

        @Size(max = 25)
        public String phone;

        @Size(max = 80)
        public String vehicleModel;

        @Size(max = 30)
        public String vehiclePlate;

        @Size(max = 80)
        public String payoutUpi;

        @Size(max = 160)
        public String baseAddress;
    }

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email
        public String email;

        @NotBlank(message = "Password is required")
        public String password;
    }

    public static class ProfileUpdateRequest {
        @NotBlank
        @Size(min = 2, max = 100)
        public String fullName;

        @Size(max = 25)
        public String phone;

        @Size(max = 80)
        public String vehicleModel;

        @Size(max = 30)
        public String vehiclePlate;

        @Size(max = 80)
        public String payoutUpi;

        @Size(max = 160)
        public String baseAddress;
    }

    public static class AuthResponse {
        public String token;
        public long expiresInMs;
        public UserProfileDto user;

        public AuthResponse(String token, long expiresInMs, UserProfileDto user) {
            this.token = token;
            this.expiresInMs = expiresInMs;
            this.user = user;
        }
    }

    public static class UserProfileDto {
        public Long id;
        public String fullName;
        public String email;
        public Role role;
        public String phone;
        public String vehicleModel;
        public String vehiclePlate;
        public String payoutUpi;
        public String baseAddress;
        public BigDecimal walletBalance;
    }
}
