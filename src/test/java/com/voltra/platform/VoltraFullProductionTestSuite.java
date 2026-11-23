package com.voltra.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltra.platform.dto.PlatformDtos.*;
import com.voltra.platform.model.*;
import com.voltra.platform.repository.*;
import com.voltra.platform.service.RazorpayGatewayService;
import com.voltra.platform.service.VoltraPlatformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VoltraFullProductionTestSuite {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepo;
    @Autowired private ChargerStationRepository stationRepo;
    @Autowired private BookingRepository bookingRepo;
    @Autowired private LedgerEntryRepository ledgerRepo;
    @Autowired private HostPayoutRepository payoutRepo;
    @Autowired private ReviewRepository reviewRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private VoltraPlatformService platformService;

    @SpyBean private RazorpayGatewayService razorpayService;

    private User driverUser;
    private User hostUser;
    private User adminUser;
    private ChargerStation approvedStation;

    @BeforeEach
    void setup() {
        reviewRepo.deleteAll();
        payoutRepo.deleteAll();
        ledgerRepo.deleteAll();
        bookingRepo.deleteAll();
        stationRepo.deleteAll();
        userRepo.deleteAll();

        driverUser = new User();
        driverUser.setFullName("Test Driver");
        driverUser.setEmail("driver@test.in");
        driverUser.setPasswordHash(passwordEncoder.encode("Driver1234"));
        driverUser.setRole(Role.DRIVER);
        driverUser = userRepo.save(driverUser);

        hostUser = new User();
        hostUser.setFullName("Test Host");
        hostUser.setEmail("host@test.in");
        hostUser.setPasswordHash(passwordEncoder.encode("Host1234"));
        hostUser.setRole(Role.HOST);
        hostUser.setPayoutUpi("testhost@okicici");
        hostUser = userRepo.save(hostUser);

        adminUser = new User();
        adminUser.setFullName("Test Admin");
        adminUser.setEmail("admin@test.in");
        adminUser.setPasswordHash(passwordEncoder.encode("Admin1234"));
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepo.save(adminUser);

        approvedStation = new ChargerStation();
        approvedStation.setName("Approved Commercial Hub");
        approvedStation.setHost(hostUser);
        approvedStation.setHostName(hostUser.getFullName());
        approvedStation.setAddress("MG Road");
        approvedStation.setCity("Bengaluru");
        approvedStation.setLatitude(12.975);
        approvedStation.setLongitude(77.605);
        approvedStation.setCategory(StationCategory.PRIVATE_COMMERCIAL);
        approvedStation.setConnectorType("CCS2");
        approvedStation.setPowerKw(50.0);
        approvedStation.setPricePerHour(BigDecimal.valueOf(200.00));
        approvedStation.setPricePerKwh(BigDecimal.valueOf(15.00));
        approvedStation.setTotalBays(1);
        approvedStation.setOnline(true);
        approvedStation.setApprovalStatus(StationApprovalStatus.APPROVED);
        approvedStation = stationRepo.save(approvedStation);

        Mockito.doReturn("order_test_999001").when(razorpayService).createOrder(any(), anyString());
        Mockito.doReturn("rfnd_test_555001").when(razorpayService).issueFullRefund(anyString());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testRoleAuthorizationAndUserDtoSanitization() throws Exception {
        String driverToken = loginAndGetToken("driver@test.in", "Driver1234");
        String hostToken = loginAndGetToken("host@test.in", "Host1234");
        String adminToken = loginAndGetToken("admin@test.in", "Admin1234");

        mockMvc.perform(get("/api/host/chargers").header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void testEndToEndBookingPaymentMeterBillingAndReviewAverage() throws Exception {
        String driverToken = loginAndGetToken("driver@test.in", "Driver1234");

        LocalDateTime start = platformService.nowIst().plusHours(1);
        LocalDateTime end = start.plusHours(2);

        BookingCreateRequest bookReq = new BookingCreateRequest();
        bookReq.chargerId = approvedStation.getId();
        bookReq.startTime = start;
        bookReq.endTime = end;

        MvcResult bookRes = mockMvc.perform(post("/api/driver/bookings")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booking.status").value("PENDING_PAYMENT"))
                .andReturn();

        long bookingId = objectMapper.readTree(bookRes.getResponse().getContentAsString())
                .get("booking").get("id").asLong();

        mockMvc.perform(post("/api/driver/bookings")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isConflict());

        PaymentVerifyRequest payReq = new PaymentVerifyRequest();
        payReq.bookingId = bookingId;
        payReq.razorpayOrderId = "order_test_999001";
        payReq.razorpayPaymentId = "pay_test_777001";
        payReq.razorpaySignature = hmacSha256("order_test_999001|pay_test_777001", "test_secret_voltra_987654321");

        MvcResult verifyRes = mockMvc.perform(post("/api/driver/payments/verify")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andReturn();

        String pin = objectMapper.readTree(verifyRes.getResponse().getContentAsString()).get("oneTimePin").asText();

        OtpVerifyRequest otpReq = new OtpVerifyRequest();
        otpReq.otp = pin;
        mockMvc.perform(post("/api/driver/bookings/" + bookingId + "/start")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHARGING"));

        MeterTelemetryRequest meterReq = new MeterTelemetryRequest();
        meterReq.bookingId = bookingId;
        meterReq.meterStartKwh = 100.0;
        meterReq.meterEndKwh = 120.0;

        mockMvc.perform(post("/api/iot/meter")
                        .header("X-Device-Api-Key", "test_esp32_api_key_999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(meterReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualKwh").value(20.0))
                .andExpect(jsonPath("$.finalBilledAmount").value(300.0));

        mockMvc.perform(post("/api/driver/bookings/" + bookingId + "/complete")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        User updatedHost = userRepo.findById(hostUser.getId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(264.00).compareTo(updatedHost.getWalletBalance()));

        ReviewCreateRequest revReq = new ReviewCreateRequest();
        revReq.bookingId = bookingId;
        revReq.rating = 4;
        revReq.comment = "Fast CCS2 charger and accurate kWh metering.";

        mockMvc.perform(post("/api/driver/reviews")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revReq)))
                .andExpect(status().isOk());

        ChargerStation updatedStation = stationRepo.findById(approvedStation.getId()).orElseThrow();
        assertEquals(1, updatedStation.getReviewCount());
        assertEquals(4.0, updatedStation.getAverageRating());
    }
}
