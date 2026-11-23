package com.voltra.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "charger_id", nullable = false)
    private ChargerStation charger;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Double durationHours;

    @Column(nullable = false)
    private Double estimatedKwh;

    @Column
    private Double meterStartKwh;

    @Column
    private Double meterEndKwh;

    @Column
    private Double actualKwh;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal finalBilledAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal platformCommission;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal hostNetAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.AWAITING_PAYMENT;

    @Column(unique = true, length = 80)
    private String razorpayOrderId;

    @Column(length = 80)
    private String razorpayPaymentId;

    @Column(length = 80)
    private String razorpayRefundId;

    @JsonIgnore
    @Column
    private String otpHash;

    @Column
    private LocalDateTime otpExpiresAt;

    @JsonIgnore
    @Column(nullable = false)
    private Integer failedOtpAttempts = 0;

    @Column(nullable = false)
    private Boolean reviewed = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ChargerStation getCharger() { return charger; }
    public void setCharger(ChargerStation charger) { this.charger = charger; }
    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Double getDurationHours() { return durationHours; }
    public void setDurationHours(Double durationHours) { this.durationHours = durationHours; }
    public Double getEstimatedKwh() { return estimatedKwh; }
    public void setEstimatedKwh(Double estimatedKwh) { this.estimatedKwh = estimatedKwh; }
    public Double getMeterStartKwh() { return meterStartKwh; }
    public void setMeterStartKwh(Double meterStartKwh) { this.meterStartKwh = meterStartKwh; }
    public Double getMeterEndKwh() { return meterEndKwh; }
    public void setMeterEndKwh(Double meterEndKwh) { this.meterEndKwh = meterEndKwh; }
    public Double getActualKwh() { return actualKwh; }
    public void setActualKwh(Double actualKwh) { this.actualKwh = actualKwh; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getFinalBilledAmount() { return finalBilledAmount; }
    public void setFinalBilledAmount(BigDecimal finalBilledAmount) { this.finalBilledAmount = finalBilledAmount; }
    public BigDecimal getPlatformCommission() { return platformCommission; }
    public void setPlatformCommission(BigDecimal platformCommission) { this.platformCommission = platformCommission; }
    public BigDecimal getHostNetAmount() { return hostNetAmount; }
    public void setHostNetAmount(BigDecimal hostNetAmount) { this.hostNetAmount = hostNetAmount; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public String getRazorpayRefundId() { return razorpayRefundId; }
    public void setRazorpayRefundId(String razorpayRefundId) { this.razorpayRefundId = razorpayRefundId; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public LocalDateTime getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) { this.otpExpiresAt = otpExpiresAt; }
    public Integer getFailedOtpAttempts() { return failedOtpAttempts; }
    public void setFailedOtpAttempts(Integer failedOtpAttempts) { this.failedOtpAttempts = failedOtpAttempts; }
    public Boolean getReviewed() { return reviewed; }
    public void setReviewed(Boolean reviewed) { this.reviewed = reviewed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
