package com.voltra.platform.controller;

import com.voltra.platform.dto.AuthDtos.UserProfileDto;
import com.voltra.platform.dto.PlatformDtos.*;
import com.voltra.platform.model.HostPayout;
import com.voltra.platform.model.LedgerEntry;
import com.voltra.platform.model.User;
import com.voltra.platform.security.RateLimiterService;
import com.voltra.platform.service.AuthService;
import com.voltra.platform.service.VoltraPlatformService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VoltraController {
    private final VoltraPlatformService service;
    private final AuthService authService;
    private final RateLimiterService rateLimiter;

    public VoltraController(VoltraPlatformService service,
                            AuthService authService,
                            RateLimiterService rateLimiter) {
        this.service = service;
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/chargers")
    public ResponseEntity<List<StationDto>> getChargers(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(service.listApprovedStations(q));
    }

    @GetMapping("/chargers/{id}/reviews")
    public ResponseEntity<List<ReviewDto>> getChargerReviews(@PathVariable Long id) {
        return ResponseEntity.ok(service.getStationReviews(id));
    }

    @PostMapping("/payments/webhook")
    public ResponseEntity<Map<String, String>> razorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        service.handleRazorpayWebhook(rawPayload, signature);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/iot/meter")
    public ResponseEntity<BookingDto> submitMeterTelemetry(
            @RequestHeader(value = "X-Device-Api-Key", required = false) String apiKey,
            @Valid @RequestBody MeterTelemetryRequest req) {
        return ResponseEntity.ok(service.recordMeterTelemetry(apiKey, req));
    }

    // DRIVER Endpoints
    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/driver/bookings")
    public ResponseEntity<List<BookingDto>> getMyBookings(@AuthenticationPrincipal User driver) {
        return ResponseEntity.ok(service.getDriverBookings(driver));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/driver/bookings")
    public ResponseEntity<BookingOrderResponse> createBooking(@AuthenticationPrincipal User driver,
                                                              @Valid @RequestBody BookingCreateRequest req) {
        return ResponseEntity.ok(service.initiateBookingWithPaymentOrder(driver, req));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/driver/payments/verify")
    public ResponseEntity<OtpIssuedResponse> verifyPayment(@AuthenticationPrincipal User driver,
                                                           @Valid @RequestBody PaymentVerifyRequest req) {
        return ResponseEntity.ok(service.verifyCheckoutPayment(driver, req));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/driver/bookings/{id}/regenerate-pin")
    public ResponseEntity<?> regeneratePin(@AuthenticationPrincipal User driver, @PathVariable Long id) {
        if (!rateLimiter.allowOtpAttempt("regen:" + driver.getId())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("status", 429, "message", "Too many PIN regeneration requests."));
        }
        return ResponseEntity.ok(service.regenerateDriverOtp(driver, id));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/driver/bookings/{id}/start")
    public ResponseEntity<?> startCharging(@AuthenticationPrincipal User driver,
                                           @PathVariable Long id,
                                           @Valid @RequestBody OtpVerifyRequest req) {
        if (!rateLimiter.allowOtpAttempt("verify:" + driver.getId())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("status", 429, "message", "Too many PIN verification attempts. Try again in 5 minutes."));
        }
        return ResponseEntity.ok(service.verifyOtpAndStartCharge(driver, id, req.otp));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/driver/bookings/{id}/complete")
    public ResponseEntity<BookingDto> completeCharging(@AuthenticationPrincipal User driver,
                                                       @PathVariable Long id) {
        return ResponseEntity.ok(service.completeCharge(driver, id));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/driver/bookings/{id}/cancel")
    public ResponseEntity<BookingDto> cancelBooking(@AuthenticationPrincipal User driver,
                                                    @PathVariable Long id) {
        return ResponseEntity.ok(service.cancelDriverBooking(driver, id));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/driver/transactions")
    public ResponseEntity<List<LedgerEntry>> getDriverTransactions(@AuthenticationPrincipal User driver) {
        return ResponseEntity.ok(service.getDriverTransactions(driver));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/driver/reviews")
    public ResponseEntity<ReviewDto> submitReview(@AuthenticationPrincipal User driver,
                                                  @Valid @RequestBody ReviewCreateRequest req) {
        return ResponseEntity.ok(service.submitReview(driver, req));
    }

    // HOST Endpoints
    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/host/chargers")
    public ResponseEntity<List<StationDto>> getHostChargers(@AuthenticationPrincipal User host) {
        return ResponseEntity.ok(service.listHostStations(host));
    }

    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/host/chargers")
    public ResponseEntity<StationDto> addHostCharger(@AuthenticationPrincipal User host,
                                                     @Valid @RequestBody StationRequest req) {
        return ResponseEntity.ok(service.createHostStation(host, req));
    }

    @PreAuthorize("hasRole('HOST')")
    @PutMapping("/host/chargers/{id}")
    public ResponseEntity<StationDto> updateHostCharger(@AuthenticationPrincipal User host,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody StationRequest req) {
        return ResponseEntity.ok(service.updateHostStation(host, id, req));
    }

    @PreAuthorize("hasRole('HOST')")
    @PatchMapping("/host/chargers/{id}/toggle")
    public ResponseEntity<StationDto> toggleHostCharger(@AuthenticationPrincipal User host,
                                                        @PathVariable Long id) {
        return ResponseEntity.ok(service.toggleStationStatus(host, id));
    }

    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/host/bookings")
    public ResponseEntity<List<BookingDto>> getHostBookings(@AuthenticationPrincipal User host) {
        return ResponseEntity.ok(service.getHostBookings(host));
    }

    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/host/bookings/{id}/approve")
    public ResponseEntity<BookingDto> approveBooking(@AuthenticationPrincipal User host,
                                                     @PathVariable Long id) {
        return ResponseEntity.ok(service.decideBooking(host, id, true));
    }

    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/host/bookings/{id}/reject")
    public ResponseEntity<BookingDto> rejectBooking(@AuthenticationPrincipal User host,
                                                    @PathVariable Long id) {
        return ResponseEntity.ok(service.decideBooking(host, id, false));
    }

    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/host/earnings")
    public ResponseEntity<HostEarningsSummaryDto> getHostEarnings(@AuthenticationPrincipal User host) {
        return ResponseEntity.ok(service.getHostEarningsSummary(host));
    }

    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/host/payouts")
    public ResponseEntity<HostPayout> requestPayout(@AuthenticationPrincipal User host,
                                                    @Valid @RequestBody PayoutRequestDto req) {
        return ResponseEntity.ok(service.requestHostPayout(host, req));
    }

    // ADMIN Endpoints
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<List<UserProfileDto>> adminUsers() {
        return ResponseEntity.ok(service.adminListUsers(authService));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/chargers")
    public ResponseEntity<List<StationDto>> adminStations() {
        return ResponseEntity.ok(service.adminListAllStations());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/chargers/{id}/approve")
    public ResponseEntity<StationDto> adminApproveStation(@PathVariable Long id) {
        return ResponseEntity.ok(service.adminDecideStation(id, true));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/chargers/{id}/reject")
    public ResponseEntity<StationDto> adminRejectStation(@PathVariable Long id) {
        return ResponseEntity.ok(service.adminDecideStation(id, false));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/bookings")
    public ResponseEntity<List<BookingDto>> adminBookings() {
        return ResponseEntity.ok(service.adminListAllBookings());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/payouts/{id}/settle")
    public ResponseEntity<HostPayout> adminSettlePayout(@PathVariable Long id) {
        return ResponseEntity.ok(service.adminSettlePayout(id));
    }
}
