package com.voltra.platform.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "charger_stations")
public class ChargerStation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 100)
    private String hostName;

    @Column(length = 30)
    private String hostPhone;

    @Column(nullable = false, length = 220)
    private String address;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StationCategory category = StationCategory.P2P_HOME;

    @Column(nullable = false, length = 40)
    private String connectorType;

    @Column(nullable = false)
    private Double powerKw;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerKwh = BigDecimal.valueOf(18.00);

    @Column(nullable = false)
    private Integer totalBays = 1;

    @Column(nullable = false)
    private Boolean online = true;

    @Column(nullable = false)
    private Boolean relayActive = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StationApprovalStatus approvalStatus = StationApprovalStatus.PENDING_VERIFICATION;

    @Column(nullable = false)
    private Double averageRating = 5.0;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    @Column(length = 160)
    private String bookedSlotsPreview = "No upcoming bookings";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public String getHostPhone() { return hostPhone; }
    public void setHostPhone(String hostPhone) { this.hostPhone = hostPhone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public StationCategory getCategory() { return category; }
    public void setCategory(StationCategory category) { this.category = category; }
    public String getConnectorType() { return connectorType; }
    public void setConnectorType(String connectorType) { this.connectorType = connectorType; }
    public Double getPowerKw() { return powerKw; }
    public void setPowerKw(Double powerKw) { this.powerKw = powerKw; }
    public BigDecimal getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }
    public BigDecimal getPricePerKwh() { return pricePerKwh; }
    public void setPricePerKwh(BigDecimal pricePerKwh) { this.pricePerKwh = pricePerKwh; }
    public Integer getTotalBays() { return totalBays; }
    public void setTotalBays(Integer totalBays) { this.totalBays = totalBays; }
    public Boolean getOnline() { return online; }
    public void setOnline(Boolean online) { this.online = online; }
    public Boolean getRelayActive() { return relayActive; }
    public void setRelayActive(Boolean relayActive) { this.relayActive = relayActive; }
    public StationApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(StationApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public String getBookedSlotsPreview() { return bookedSlotsPreview; }
    public void setBookedSlotsPreview(String bookedSlotsPreview) { this.bookedSlotsPreview = bookedSlotsPreview; }
    public User getHost() { return host; }
    public void setHost(User host) { this.host = host; }
}
