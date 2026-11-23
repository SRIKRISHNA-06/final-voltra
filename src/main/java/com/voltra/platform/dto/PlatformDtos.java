package com.voltra.platform.dto;

import com.voltra.platform.model.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PlatformDtos {
    public static class StationRequest {
        @NotBlank @Size(min = 3, max = 120)
        public String name;

        @Size(max = 30)
        public String hostPhone;

        @NotBlank @Size(min = 4, max = 220)
        public String address;

        @NotBlank @Size(min = 2, max = 80)
        public String city;

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        public Double latitude;

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        public Double longitude;

        public StationCategory category = StationCategory.P2P_HOME;

        @NotBlank @Size(max = 40)
        public String connectorType;

        @NotNull @DecimalMin("1.0") @DecimalMax("500.0")
        public Double powerKw;

        @NotNull @DecimalMin("10.0") @DecimalMax("5000.0")
        public BigDecimal pricePerHour;

        @DecimalMin("1.0") @DecimalMax("500.0")
        public BigDecimal pricePerKwh = BigDecimal.valueOf(18.00);

        @Min(1) @Max(50)
        public Integer totalBays = 1;

        public Boolean online = true;
    }

    public static class StationDto {
        public Long id;
        public Long hostId;
        public String name;
        public String hostName;
        public String hostPhone;
        public String address;
        public String city;
        public Double latitude;
        public Double longitude;
        public StationCategory category;
        public String connectorType;
        public Double powerKw;
        public BigDecimal pricePerHour;
        public BigDecimal pricePerKwh;
        public Integer totalBays;
        public Integer availableBays;
        public Boolean online;
        public Boolean relayActive;
        public StationApprovalStatus approvalStatus;
        public Boolean verified;
        public Double averageRating;
        public Integer reviewCount;
        public String nextFreeTime;
        public String bookedSlotsPreview;
    }

    public static class BookingCreateRequest {
        @NotNull
        public Long chargerId;

        @NotNull
        public LocalDateTime startTime;

        @NotNull
        public LocalDateTime endTime;
    }

    public static class BookingOrderResponse {
        public BookingDto booking;
        public String upiTxnRef;
        public String upiVpa;
        public String upiPayeeName;
        public String upiUri;
        public BigDecimal amountInr;
        public String currency;
        // Backward-compatible aliases
        public String razorpayOrderId;
        public String razorpayKeyId;
        public long amountPaise;
    }

    public static class PaymentVerifyRequest {
        @NotNull
        public Long bookingId;

        public String upiTxnRef;

        @Pattern(regexp = "^[A-Za-z0-9]{8,35}$", message = "Enter a valid 12-digit UPI UTR / Reference number")
        public String upiUtr;

        // Backward-compatible fields
        public String razorpayOrderId;
        public String razorpayPaymentId;
        public String razorpaySignature;
    }

    public static class OtpVerifyRequest {
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be a 6-digit numeric code")
        public String otp;
    }

    public static class MeterTelemetryRequest {
        @NotNull
        public Long bookingId;

        @NotNull @DecimalMin("0.0")
        public Double meterStartKwh;

        @NotNull @DecimalMin("0.0")
        public Double meterEndKwh;
    }

    public static class OtpIssuedResponse {
        public Long bookingId;
        public String oneTimePin;
        public LocalDateTime expiresAt;
        public String notice;
        public String verificationStatus;
    }

    public static class BookingDto {
        public Long id;
        public Long chargerId;
        public String chargerName;
        public String chargerAddress;
        public String chargerCity;
        public Double chargerLat;
        public Double chargerLng;
        public String connectorType;
        public Double powerKw;
        public BigDecimal pricePerHour;
        public BigDecimal pricePerKwh;
        public StationCategory category;
        public Long driverId;
        public String driverName;
        public String driverVehicle;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public Double durationHours;
        public Double estimatedKwh;
        public Double meterStartKwh;
        public Double meterEndKwh;
        public Double actualKwh;
        public BigDecimal grossAmount;
        public BigDecimal finalBilledAmount;
        public BigDecimal platformCommission;
        public BigDecimal hostNetAmount;
        public BookingStatus status;
        public PaymentStatus paymentStatus;
        public String razorpayOrderId;
        public String razorpayPaymentId;
        public String razorpayRefundId;
        public LocalDateTime otpExpiresAt;
        public Boolean otpActive;
        public Boolean reviewed;
    }

    public static class HostEarningsSummaryDto {
        public BigDecimal walletBalance;
        public BigDecimal totalGrossEarned;
        public BigDecimal totalCommissionPaid;
        public BigDecimal totalNetEarned;
        public String payoutUpi;
        public List<LedgerEntry> ledgerEntries;
        public List<HostPayout> payouts;
    }

    public static class PayoutRequestDto {
        @NotNull @DecimalMin("100.00")
        public BigDecimal amount;

        @Size(max = 80)
        public String payoutUpi;
    }

    public static class ReviewCreateRequest {
        @NotNull
        public Long bookingId;

        @NotNull @Min(1) @Max(5)
        public Integer rating;

        @NotBlank @Size(min = 3, max = 500)
        public String comment;
    }

    public static class ReviewDto {
        public Long id;
        public String driverName;
        public String vehicleModel;
        public Integer rating;
        public String comment;
        public LocalDateTime createdAt;
    }
}
