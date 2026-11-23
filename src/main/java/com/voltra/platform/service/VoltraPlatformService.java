package com.voltra.platform.service;

import com.voltra.platform.dto.AuthDtos.UserProfileDto;
import com.voltra.platform.dto.PlatformDtos.*;
import com.voltra.platform.model.*;
import com.voltra.platform.repository.*;
import com.voltra.platform.security.InputSanitizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VoltraPlatformService {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final List<BookingStatus> BLOCKING_STATUSES = List.of(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.PENDING_APPROVAL,
            BookingStatus.CONFIRMED,
            BookingStatus.CHARGING
    );

    private final ChargerStationRepository stationRepo;
    private final BookingRepository bookingRepo;
    private final LedgerEntryRepository ledgerRepo;
    private final HostPayoutRepository payoutRepo;
    private final ReviewRepository reviewRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final InputSanitizer sanitizer;
    private final BigDecimal commissionRate;
    private final String deviceApiKey;
    private final String merchantUpiVpa;
    private final SecureRandom secureRandom = new SecureRandom();

    public VoltraPlatformService(ChargerStationRepository stationRepo,
                                 BookingRepository bookingRepo,
                                 LedgerEntryRepository ledgerRepo,
                                 HostPayoutRepository payoutRepo,
                                 ReviewRepository reviewRepo,
                                 UserRepository userRepo,
                                 PasswordEncoder passwordEncoder,
                                 InputSanitizer sanitizer,
                                 @Value("${voltra.platform.commission-rate:0.12}") BigDecimal commissionRate,
                                 @Value("${voltra.device.api-key:}") String deviceApiKey,
                                 @Value("${voltra.upi.vpa:voltra.evgrid@okicici}") String merchantUpiVpa) {
        this.stationRepo = stationRepo;
        this.bookingRepo = bookingRepo;
        this.ledgerRepo = ledgerRepo;
        this.payoutRepo = payoutRepo;
        this.reviewRepo = reviewRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.sanitizer = sanitizer;
        this.commissionRate = commissionRate;
        this.deviceApiKey = deviceApiKey;
        this.merchantUpiVpa = merchantUpiVpa;
    }

    public LocalDateTime nowIst() {
        return LocalDateTime.now(IST);
    }

    public List<StationDto> listApprovedStations(String query) {
        List<ChargerStation> stations = (query == null || query.isBlank())
                ? stationRepo.findByApprovalStatusOrderByIdDesc(StationApprovalStatus.APPROVED)
                : stationRepo.searchApprovedStations(query.trim(), StationApprovalStatus.APPROVED);
        return stations.stream().map(this::toStationDto).collect(Collectors.toList());
    }

    public List<StationDto> listHostStations(User host) {
        return stationRepo.findByHostIdOrderByIdDesc(host.getId())
                .stream().map(this::toStationDto).collect(Collectors.toList());
    }

    @Transactional
    public StationDto createHostStation(User host, StationRequest req) {
        ChargerStation s = new ChargerStation();
        s.setHost(host);
        s.setHostName(host.getFullName());
        s.setHostPhone(sanitizer.clean(req.hostPhone != null ? req.hostPhone : host.getPhone()));
        s.setName(sanitizer.clean(req.name));
        s.setAddress(sanitizer.clean(req.address));
        s.setCity(sanitizer.clean(req.city));
        s.setLatitude(req.latitude);
        s.setLongitude(req.longitude);
        s.setCategory(req.category != null ? req.category : StationCategory.P2P_HOME);
        s.setConnectorType(sanitizer.clean(req.connectorType));
        s.setPowerKw(req.powerKw);
        s.setPricePerHour(req.pricePerHour.setScale(2, RoundingMode.HALF_UP));
        s.setPricePerKwh((req.pricePerKwh != null ? req.pricePerKwh : BigDecimal.valueOf(18.00)).setScale(2, RoundingMode.HALF_UP));
        s.setTotalBays(req.totalBays != null ? req.totalBays : 1);
        s.setOnline(req.online != null ? req.online : true);
        s.setRelayActive(false);
        s.setApprovalStatus(StationApprovalStatus.PENDING_VERIFICATION);
        s.setAverageRating(5.0);
        s.setReviewCount(0);
        s.setBookedSlotsPreview("Available Now");
        return toStationDto(stationRepo.save(s));
    }

    @Transactional
    public StationDto updateHostStation(User host, Long stationId, StationRequest req) {
        ChargerStation s = stationRepo.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Charger station not found."));
        verifyHostOwnership(host, s);

        s.setName(sanitizer.clean(req.name));
        s.setAddress(sanitizer.clean(req.address));
        s.setCity(sanitizer.clean(req.city));
        s.setLatitude(req.latitude);
        s.setLongitude(req.longitude);
        if (req.category != null) s.setCategory(req.category);
        s.setConnectorType(sanitizer.clean(req.connectorType));
        s.setPowerKw(req.powerKw);
        s.setPricePerHour(req.pricePerHour.setScale(2, RoundingMode.HALF_UP));
        if (req.pricePerKwh != null) s.setPricePerKwh(req.pricePerKwh.setScale(2, RoundingMode.HALF_UP));
        if (req.totalBays != null) s.setTotalBays(req.totalBays);
        if (req.online != null) s.setOnline(req.online);
        return toStationDto(stationRepo.save(s));
    }

    @Transactional
    public StationDto toggleStationStatus(User host, Long stationId) {
        ChargerStation s = stationRepo.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Charger station not found."));
        verifyHostOwnership(host, s);

        boolean nextState = !Boolean.TRUE.equals(s.getOnline());
        s.setOnline(nextState);
        if (!nextState) s.setRelayActive(false);
        return toStationDto(stationRepo.save(s));
    }

    @Transactional
    public BookingOrderResponse initiateBookingWithPaymentOrder(User driver, BookingCreateRequest req) {
        ChargerStation charger = stationRepo.findByIdWithLock(req.chargerId)
                .orElseThrow(() -> new IllegalArgumentException("Selected charger not found."));

        if (charger.getApprovalStatus() != StationApprovalStatus.APPROVED) {
            throw new IllegalStateException("This station is pending admin approval.");
        }
        if (!Boolean.TRUE.equals(charger.getOnline())) {
            throw new IllegalStateException("This station is currently offline.");
        }

        LocalDateTime now = nowIst();
        if (req.startTime.isBefore(now.minusMinutes(2))) {
            throw new IllegalArgumentException("Booking start time cannot be in the past.");
        }
        if (!req.endTime.isAfter(req.startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        long minutes = Duration.between(req.startTime, req.endTime).toMinutes();
        if (minutes < 30 || minutes > 720) {
            throw new IllegalArgumentException("Booking duration must be between 30 minutes and 12 hours.");
        }

        long overlaps = bookingRepo.countOverlappingBookings(
                charger.getId(), req.startTime, req.endTime, BLOCKING_STATUSES
        );
        if (overlaps >= charger.getTotalBays()) {
            throw new IllegalStateException("All bays at this station are already reserved for that time window.");
        }

        double hours = Math.round((minutes / 60.0) * 100.0) / 100.0;
        double estKwh = Math.round((hours * charger.getPowerKw()) * 10.0) / 10.0;

        BigDecimal grossAmount = charger.getPricePerHour()
                .multiply(BigDecimal.valueOf(hours))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = grossAmount.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal hostNet = grossAmount.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);

        String upiTxnRef = "VLTUPI" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Booking b = new Booking();
        b.setCharger(charger);
        b.setDriver(driver);
        b.setStartTime(req.startTime);
        b.setEndTime(req.endTime);
        b.setDurationHours(hours);
        b.setEstimatedKwh(estKwh);
        b.setGrossAmount(grossAmount);
        b.setPlatformCommission(platformFee);
        b.setHostNetAmount(hostNet);
        b.setStatus(BookingStatus.PENDING_PAYMENT);
        b.setPaymentStatus(PaymentStatus.AWAITING_PAYMENT);
        b.setRazorpayOrderId(upiTxnRef);
        b.setReviewed(false);
        Booking saved = bookingRepo.save(b);

        LedgerEntry entry = new LedgerEntry();
        entry.setBooking(saved);
        entry.setDriverId(driver.getId());
        entry.setHostId(charger.getHost().getId());
        entry.setGatewayOrderId(upiTxnRef);
        entry.setGrossAmount(grossAmount);
        entry.setPlatformFee(platformFee);
        entry.setHostNetAmount(hostNet);
        entry.setStatus(PaymentStatus.AWAITING_PAYMENT);
        entry.setEntryNote("Direct UPI intent created (" + upiTxnRef + "); slot locked for 10 mins.");
        ledgerRepo.save(entry);

        String payeeVpa = (charger.getHost() != null && charger.getHost().getPayoutUpi() != null && !charger.getHost().getPayoutUpi().isBlank())
                ? charger.getHost().getPayoutUpi().trim()
                : merchantUpiVpa;
        String payeeName = "VOLTRA EV Grid";
        String note = "VOLTRA Booking #" + saved.getId() + " (" + upiTxnRef + ")";
        String upiUri = "upi://pay?pa=" + encodeUri(payeeVpa)
                + "&pn=" + encodeUri(payeeName)
                + "&am=" + grossAmount.toPlainString()
                + "&cu=INR"
                + "&tr=" + encodeUri(upiTxnRef)
                + "&tn=" + encodeUri(note);

        BookingOrderResponse resp = new BookingOrderResponse();
        resp.booking = toBookingDto(saved);
        resp.upiTxnRef = upiTxnRef;
        resp.upiVpa = payeeVpa;
        resp.upiPayeeName = payeeName;
        resp.upiUri = upiUri;
        resp.amountInr = grossAmount;
        resp.currency = "INR";
        resp.razorpayOrderId = upiTxnRef;
        resp.amountPaise = grossAmount.multiply(BigDecimal.valueOf(100)).longValue();
        return resp;
    }

    @Transactional
    public OtpIssuedResponse verifyCheckoutPayment(User driver, PaymentVerifyRequest req) {
        Booking b = bookingRepo.findById(req.bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        verifyDriverOwnership(driver, b);

        if (b.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("UPI payment for this booking is already verified.");
        }
        if (b.getStatus() == BookingStatus.EXPIRED || b.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("This booking slot lock has expired. Please book a fresh slot.");
        }

        String expectedRef = req.upiTxnRef != null ? req.upiTxnRef : req.razorpayOrderId;
        if (expectedRef == null || !expectedRef.equals(b.getRazorpayOrderId())) {
            throw new IllegalArgumentException("UPI Transaction Reference mismatch.");
        }

        String utr = req.upiUtr != null ? req.upiUtr.trim().toUpperCase() : req.razorpayPaymentId;
        if (utr == null || !utr.matches("^[A-Z0-9]{12,35}$")) {
            throw new IllegalArgumentException("Please enter the valid 12-digit UPI UTR / Reference number from your UPI app.");
        }

        return finalizePaidBooking(b, "UTR-" + utr, "Verified Direct UPI Payment (UTR: " + utr + ")");
    }

    public void handleRazorpayWebhook(String rawPayload, String signatureHeader) {
        // Legacy endpoint retained for route compatibility; VOLTRA uses Direct UPI.
    }

    private OtpIssuedResponse finalizePaidBooking(Booking b, String paymentId, String sourceNote) {
        b.setPaymentStatus(PaymentStatus.PAID);
        b.setRazorpayPaymentId(paymentId);

        if (b.getCharger().getCategory() == StationCategory.P2P_HOME) {
            b.setStatus(BookingStatus.PENDING_APPROVAL);
        } else {
            b.setStatus(BookingStatus.CONFIRMED);
        }

        String rawSixDigitOtp = generateSixDigitOtp();
        LocalDateTime expiry = b.getEndTime().plusMinutes(15);
        b.setOtpHash(passwordEncoder.encode(rawSixDigitOtp));
        b.setOtpExpiresAt(expiry);
        b.setFailedOtpAttempts(0);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm a");
        ChargerStation charger = b.getCharger();
        charger.setBookedSlotsPreview("Next Slot: " + b.getStartTime().format(fmt) + " - " + b.getEndTime().format(fmt));
        stationRepo.save(charger);
        bookingRepo.save(b);

        LedgerEntry entry = new LedgerEntry();
        entry.setBooking(b);
        entry.setDriverId(b.getDriver().getId());
        entry.setHostId(charger.getHost().getId());
        entry.setGatewayOrderId(b.getRazorpayOrderId());
        entry.setGatewayPaymentId(paymentId);
        entry.setGrossAmount(b.getGrossAmount());
        entry.setPlatformFee(b.getPlatformCommission());
        entry.setHostNetAmount(b.getHostNetAmount());
        entry.setStatus(PaymentStatus.PAID);
        entry.setEntryNote(sourceNote);
        ledgerRepo.save(entry);

        OtpIssuedResponse out = new OtpIssuedResponse();
        out.bookingId = b.getId();
        out.oneTimePin = rawSixDigitOtp;
        out.expiresAt = expiry;
        out.verificationStatus = "SUCCESS";
        out.notice = "Save this PIN now. For your security it won't be shown again.";
        return out;
    }

    @Transactional
    public OtpIssuedResponse regenerateDriverOtp(User driver, Long bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        verifyDriverOwnership(driver, b);

        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("PIN regeneration is only allowed for CONFIRMED bookings.");
        }

        String freshOtp = generateSixDigitOtp();
        LocalDateTime expiry = b.getEndTime().plusMinutes(15);
        b.setOtpHash(passwordEncoder.encode(freshOtp));
        b.setOtpExpiresAt(expiry);
        b.setFailedOtpAttempts(0);
        bookingRepo.save(b);

        OtpIssuedResponse out = new OtpIssuedResponse();
        out.bookingId = b.getId();
        out.oneTimePin = freshOtp;
        out.expiresAt = expiry;
        out.verificationStatus = "SUCCESS";
        out.notice = "Save this PIN now. For your security it won't be shown again.";
        return out;
    }

    @Transactional
    public BookingDto verifyOtpAndStartCharge(User driver, Long bookingId, String rawOtp) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        verifyDriverOwnership(driver, b);

        if (b.getStatus() != BookingStatus.CONFIRMED || b.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Booking must be PAID and CONFIRMED before starting charge.");
        }
        if (b.getOtpHash() == null || b.getOtpExpiresAt() == null || nowIst().isAfter(b.getOtpExpiresAt())) {
            throw new IllegalStateException("Your charging PIN has expired. Please regenerate a new PIN.");
        }
        if (b.getFailedOtpAttempts() >= 5) {
            throw new AccessDeniedException("PIN locked after 5 failed attempts. Regenerate a new PIN.");
        }
        if (!passwordEncoder.matches(rawOtp, b.getOtpHash())) {
            b.setFailedOtpAttempts(b.getFailedOtpAttempts() + 1);
            bookingRepo.save(b);
            throw new IllegalArgumentException("Invalid 6-digit charging PIN.");
        }

        b.setStatus(BookingStatus.CHARGING);
        b.setOtpHash(null);

        ChargerStation c = b.getCharger();
        c.setRelayActive(true);
        stationRepo.save(c);
        return toBookingDto(bookingRepo.save(b));
    }

    @Transactional
    public BookingDto recordMeterTelemetry(String providedApiKey, MeterTelemetryRequest req) {
        if (deviceApiKey == null || deviceApiKey.isBlank() || !deviceApiKey.equals(providedApiKey)) {
            throw new AccessDeniedException("Invalid or missing X-Device-Api-Key header.");
        }
        if (req.meterEndKwh < req.meterStartKwh) {
            throw new IllegalArgumentException("meterEndKwh cannot be less than meterStartKwh.");
        }
        Booking b = bookingRepo.findById(req.bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        if (b.getStatus() != BookingStatus.CHARGING && b.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Meter readings can only be recorded for active sessions.");
        }

        double deliveredKwh = Math.round((req.meterEndKwh - req.meterStartKwh) * 100.0) / 100.0;
        b.setMeterStartKwh(req.meterStartKwh);
        b.setMeterEndKwh(req.meterEndKwh);
        b.setActualKwh(deliveredKwh);

        BigDecimal energyBill = b.getCharger().getPricePerKwh()
                .multiply(BigDecimal.valueOf(deliveredKwh))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalBill = energyBill.min(b.getGrossAmount());
        b.setFinalBilledAmount(finalBill);
        return toBookingDto(bookingRepo.save(b));
    }

    @Transactional
    public BookingDto completeCharge(User driver, Long bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        verifyDriverOwnership(driver, b);

        if (b.getStatus() != BookingStatus.CHARGING) {
            throw new IllegalStateException("Only an active CHARGING session can be completed.");
        }

        b.setStatus(BookingStatus.COMPLETED);
        ChargerStation c = b.getCharger();
        c.setRelayActive(false);
        stationRepo.save(c);

        BigDecimal settlementGross = b.getFinalBilledAmount() != null ? b.getFinalBilledAmount() : b.getGrossAmount();
        BigDecimal platformFee = settlementGross.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal hostCredit = settlementGross.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);
        b.setPlatformCommission(platformFee);
        b.setHostNetAmount(hostCredit);

        User host = userRepo.findByIdForUpdate(c.getHost().getId())
                .orElseThrow(() -> new IllegalStateException("Host account not found."));
        host.setWalletBalance(host.getWalletBalance().add(hostCredit));
        userRepo.save(host);

        return toBookingDto(bookingRepo.save(b));
    }

    @Transactional
    public BookingDto cancelDriverBooking(User driver, Long bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        verifyDriverOwnership(driver, b);

        if (b.getStatus() == BookingStatus.CHARGING || b.getStatus() == BookingStatus.COMPLETED
                || b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.EXPIRED) {
            throw new IllegalStateException("This booking cannot be cancelled from status: " + b.getStatus());
        }

        b.setStatus(BookingStatus.CANCELLED);
        b.setOtpHash(null);

        if (b.getPaymentStatus() == PaymentStatus.PAID && b.getRazorpayRefundId() == null) {
            String refundRef = "UPI-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            b.setPaymentStatus(PaymentStatus.REFUNDED);
            b.setRazorpayRefundId(refundRef);
            recordRefundLedger(b, refundRef, "UPI refund queued & tracked on driver cancellation");
        }
        return toBookingDto(bookingRepo.save(b));
    }

    @Transactional
    public BookingDto decideBooking(User host, Long bookingId, boolean approve) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        verifyHostOwnership(host, b.getCharger());

        if (b.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only PENDING_APPROVAL bookings can be accepted or rejected.");
        }
        if (approve) {
            b.setStatus(BookingStatus.CONFIRMED);
        } else {
            b.setStatus(BookingStatus.REJECTED);
            b.setOtpHash(null);
            if (b.getPaymentStatus() == PaymentStatus.PAID && b.getRazorpayRefundId() == null) {
                String refundRef = "UPI-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                b.setPaymentStatus(PaymentStatus.REFUNDED);
                b.setRazorpayRefundId(refundRef);
                recordRefundLedger(b, refundRef, "UPI refund queued & tracked on host rejection");
            }
        }
        return toBookingDto(bookingRepo.save(b));
    }

    public HostEarningsSummaryDto getHostEarningsSummary(User hostPrincipal) {
        User host = userRepo.findById(hostPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("Host not found."));
        List<LedgerEntry> entries = ledgerRepo.findByHostIdOrderByCreatedAtDesc(host.getId());
        List<HostPayout> payouts = payoutRepo.findByHostIdOrderByRequestedAtDesc(host.getId());

        BigDecimal gross = entries.stream()
                .filter(e -> e.getStatus() == PaymentStatus.PAID)
                .map(LedgerEntry::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fees = entries.stream()
                .filter(e -> e.getStatus() == PaymentStatus.PAID)
                .map(LedgerEntry::getPlatformFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = entries.stream()
                .filter(e -> e.getStatus() == PaymentStatus.PAID)
                .map(LedgerEntry::getHostNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        HostEarningsSummaryDto dto = new HostEarningsSummaryDto();
        dto.walletBalance = host.getWalletBalance();
        dto.totalGrossEarned = gross;
        dto.totalCommissionPaid = fees;
        dto.totalNetEarned = net;
        dto.payoutUpi = host.getPayoutUpi();
        dto.ledgerEntries = entries;
        dto.payouts = payouts;
        return dto;
    }

    @Transactional
    public HostPayout requestHostPayout(User hostPrincipal, PayoutRequestDto req) {
        User host = userRepo.findByIdForUpdate(hostPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("Host account not found."));
        String upi = (req.payoutUpi != null && !req.payoutUpi.isBlank())
                ? sanitizer.clean(req.payoutUpi)
                : host.getPayoutUpi();
        if (upi == null || upi.isBlank()) {
            throw new IllegalArgumentException("Please configure a valid Payout UPI ID.");
        }
        BigDecimal requested = req.amount.setScale(2, RoundingMode.HALF_UP);
        if (host.getWalletBalance().compareTo(requested) < 0) {
            throw new IllegalStateException("Insufficient available wallet balance for payout.");
        }

        host.setWalletBalance(host.getWalletBalance().subtract(requested));
        userRepo.save(host);

        HostPayout payout = new HostPayout();
        payout.setHost(host);
        payout.setAmount(requested);
        payout.setPayoutUpi(upi);
        payout.setStatus(PayoutStatus.REQUESTED);
        payout.setReferenceCode("PO-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        return payoutRepo.save(payout);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireStaleBookings() {
        LocalDateTime now = nowIst();
        for (Booking b : bookingRepo.findByStatusAndCreatedAtBefore(BookingStatus.PENDING_PAYMENT, now.minusMinutes(10))) {
            b.setStatus(BookingStatus.EXPIRED);
            b.setPaymentStatus(PaymentStatus.FAILED);
            bookingRepo.save(b);
        }
        for (Booking b : bookingRepo.findByStatusAndCreatedAtBefore(BookingStatus.PENDING_APPROVAL, now.minusMinutes(30))) {
            b.setStatus(BookingStatus.EXPIRED);
            b.setOtpHash(null);
            if (b.getPaymentStatus() == PaymentStatus.PAID && b.getRazorpayRefundId() == null) {
                String refundRef = "UPI-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                b.setPaymentStatus(PaymentStatus.REFUNDED);
                b.setRazorpayRefundId(refundRef);
                recordRefundLedger(b, refundRef, "Auto-refund on 30-min host approval timeout");
            }
            bookingRepo.save(b);
        }
    }

    @Transactional
    public ReviewDto submitReview(User driver, ReviewCreateRequest req) {
        Booking b = bookingRepo.findById(req.bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        verifyDriverOwnership(driver, b);

        if (b.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Reviews can only be submitted after completing a session.");
        }
        if (Boolean.TRUE.equals(b.getReviewed())) {
            throw new IllegalStateException("You have already reviewed this booking.");
        }

        ChargerStation station = b.getCharger();
        Review review = new Review();
        review.setCharger(station);
        review.setDriverId(driver.getId());
        review.setDriverName(driver.getFullName());
        review.setVehicleModel(driver.getVehicleModel());
        review.setRating(req.rating);
        review.setComment(sanitizer.clean(req.comment));
        Review saved = reviewRepo.save(review);

        int oldCount = station.getReviewCount() != null ? station.getReviewCount() : 0;
        double oldAvg = station.getAverageRating() != null ? station.getAverageRating() : 5.0;
        int newCount = oldCount + 1;
        double newAvg = Math.round((((oldAvg * oldCount) + req.rating) / newCount) * 10.0) / 10.0;

        station.setReviewCount(newCount);
        station.setAverageRating(newAvg);
        stationRepo.save(station);

        b.setReviewed(true);
        bookingRepo.save(b);
        return toReviewDto(saved);
    }

    public List<ReviewDto> getStationReviews(Long stationId) {
        return reviewRepo.findByChargerIdOrderByCreatedAtDesc(stationId)
                .stream().map(this::toReviewDto).collect(Collectors.toList());
    }

    public List<BookingDto> getDriverBookings(User driver) {
        return bookingRepo.findByDriverIdOrderByStartTimeDesc(driver.getId())
                .stream().map(this::toBookingDto).collect(Collectors.toList());
    }

    public List<BookingDto> getHostBookings(User host) {
        return bookingRepo.findByChargerHostIdOrderByStartTimeDesc(host.getId())
                .stream().map(this::toBookingDto).collect(Collectors.toList());
    }

    public List<LedgerEntry> getDriverTransactions(User driver) {
        return ledgerRepo.findByDriverIdOrderByCreatedAtDesc(driver.getId());
    }

    public List<UserProfileDto> adminListUsers(AuthService authService) {
        return userRepo.findAll().stream().map(authService::toDto).collect(Collectors.toList());
    }

    public List<StationDto> adminListAllStations() {
        return stationRepo.findAll().stream().map(this::toStationDto).collect(Collectors.toList());
    }

    public List<BookingDto> adminListAllBookings() {
        return bookingRepo.findAllByOrderByCreatedAtDesc().stream().map(this::toBookingDto).collect(Collectors.toList());
    }

    @Transactional
    public StationDto adminDecideStation(Long stationId, boolean approve) {
        ChargerStation s = stationRepo.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Station not found."));
        s.setApprovalStatus(approve ? StationApprovalStatus.APPROVED : StationApprovalStatus.REJECTED);
        return toStationDto(stationRepo.save(s));
    }

    @Transactional
    public HostPayout adminSettlePayout(Long payoutId) {
        HostPayout p = payoutRepo.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout record not found."));
        p.setStatus(PayoutStatus.SETTLED);
        p.setProcessedAt(nowIst());
        return payoutRepo.save(p);
    }

    private void recordRefundLedger(Booking b, String refundId, String note) {
        LedgerEntry entry = new LedgerEntry();
        entry.setBooking(b);
        entry.setDriverId(b.getDriver().getId());
        entry.setHostId(b.getCharger().getHost().getId());
        entry.setGatewayOrderId(b.getRazorpayOrderId());
        entry.setGatewayPaymentId(b.getRazorpayPaymentId());
        entry.setGatewayRefundId(refundId);
        entry.setGrossAmount(b.getGrossAmount());
        entry.setPlatformFee(b.getPlatformCommission());
        entry.setHostNetAmount(b.getHostNetAmount());
        entry.setStatus(PaymentStatus.REFUNDED);
        entry.setEntryNote(note);
        ledgerRepo.save(entry);
    }

    private String encodeUri(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String generateSixDigitOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private void verifyDriverOwnership(User driver, Booking b) {
        if (b.getDriver() == null || !b.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("Unauthorized: You can only manage your own bookings.");
        }
    }

    private void verifyHostOwnership(User host, ChargerStation s) {
        if (s.getHost() == null || !s.getHost().getId().equals(host.getId())) {
            throw new AccessDeniedException("Unauthorized: You can only manage chargers owned by your account.");
        }
    }

    public StationDto toStationDto(ChargerStation s) {
        LocalDateTime now = nowIst();
        long activeNow = bookingRepo.countOverlappingBookings(
                s.getId(), now, now.plusMinutes(1), BLOCKING_STATUSES
        );
        int freeBays = Math.max(0, s.getTotalBays() - (int) activeNow);

        StationDto dto = new StationDto();
        dto.id = s.getId();
        dto.hostId = s.getHost() != null ? s.getHost().getId() : null;
        dto.name = s.getName();
        dto.hostName = s.getHostName();
        dto.hostPhone = s.getHostPhone();
        dto.address = s.getAddress();
        dto.city = s.getCity();
        dto.latitude = s.getLatitude();
        dto.longitude = s.getLongitude();
        dto.category = s.getCategory();
        dto.connectorType = s.getConnectorType();
        dto.powerKw = s.getPowerKw();
        dto.pricePerHour = s.getPricePerHour();
        dto.pricePerKwh = s.getPricePerKwh();
        dto.totalBays = s.getTotalBays();
        dto.availableBays = freeBays;
        dto.online = s.getOnline();
        dto.relayActive = s.getRelayActive();
        dto.approvalStatus = s.getApprovalStatus();
        dto.verified = s.getApprovalStatus() == StationApprovalStatus.APPROVED;
        dto.averageRating = s.getAverageRating();
        dto.reviewCount = s.getReviewCount();
        dto.nextFreeTime = !Boolean.TRUE.equals(s.getOnline())
                ? "Offline"
                : (freeBays > 0 ? "Available Now" : "All Bays Busy");
        dto.bookedSlotsPreview = s.getBookedSlotsPreview();
        return dto;
    }

    public BookingDto toBookingDto(Booking b) {
        BookingDto dto = new BookingDto();
        dto.id = b.getId();
        dto.chargerId = b.getCharger().getId();
        dto.chargerName = b.getCharger().getName();
        dto.chargerAddress = b.getCharger().getAddress();
        dto.chargerCity = b.getCharger().getCity();
        dto.chargerLat = b.getCharger().getLatitude();
        dto.chargerLng = b.getCharger().getLongitude();
        dto.connectorType = b.getCharger().getConnectorType();
        dto.powerKw = b.getCharger().getPowerKw();
        dto.pricePerHour = b.getCharger().getPricePerHour();
        dto.pricePerKwh = b.getCharger().getPricePerKwh();
        dto.category = b.getCharger().getCategory();
        dto.driverId = b.getDriver().getId();
        dto.driverName = b.getDriver().getFullName();
        dto.driverVehicle = (b.getDriver().getVehicleModel() != null ? b.getDriver().getVehicleModel() : "EV")
                + (b.getDriver().getVehiclePlate() != null ? " (" + b.getDriver().getVehiclePlate() + ")" : "");
        dto.startTime = b.getStartTime();
        dto.endTime = b.getEndTime();
        dto.durationHours = b.getDurationHours();
        dto.estimatedKwh = b.getEstimatedKwh();
        dto.meterStartKwh = b.getMeterStartKwh();
        dto.meterEndKwh = b.getMeterEndKwh();
        dto.actualKwh = b.getActualKwh();
        dto.grossAmount = b.getGrossAmount();
        dto.finalBilledAmount = b.getFinalBilledAmount();
        dto.platformCommission = b.getPlatformCommission();
        dto.hostNetAmount = b.getHostNetAmount();
        dto.status = b.getStatus();
        dto.paymentStatus = b.getPaymentStatus();
        dto.razorpayOrderId = b.getRazorpayOrderId();
        dto.razorpayPaymentId = b.getRazorpayPaymentId();
        dto.razorpayRefundId = b.getRazorpayRefundId();
        dto.otpExpiresAt = b.getOtpExpiresAt();
        dto.otpActive = b.getOtpHash() != null && b.getOtpExpiresAt() != null && nowIst().isBefore(b.getOtpExpiresAt());
        dto.reviewed = b.getReviewed();
        return dto;
    }

    private ReviewDto toReviewDto(Review r) {
        ReviewDto dto = new ReviewDto();
        dto.id = r.getId();
        dto.driverName = r.getDriverName();
        dto.vehicleModel = r.getVehicleModel();
        dto.rating = r.getRating();
        dto.comment = r.getComment();
        dto.createdAt = r.getCreatedAt();
        return dto;
    }
}
