package com.voltra.platform.config;

import com.voltra.platform.model.*;
import com.voltra.platform.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepo;
    private final ChargerStationRepository stationRepo;
    private final ReviewRepository reviewRepo;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository userRepo,
                      ChargerStationRepository stationRepo,
                      ReviewRepository reviewRepo,
                      PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.stationRepo = stationRepo;
        this.reviewRepo = reviewRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (stationRepo.count() > 0) return;

        User host = new User();
        host.setFullName("Arvind Kulkarni");
        host.setEmail("host@voltra.in");
        host.setPasswordHash(encoder.encode("HostPass123"));
        host.setRole(Role.HOST);
        host.setPhone("+91 99001 44556");
        host.setPayoutUpi("arvind.voltra@okicici");
        host.setBaseAddress("Indiranagar 2nd Stage, Bengaluru");
        host = userRepo.save(host);

        User driver = new User();
        driver.setFullName("Srikrishna Rao");
        driver.setEmail("driver@voltra.in");
        driver.setPasswordHash(encoder.encode("DriverPass123"));
        driver.setRole(Role.DRIVER);
        driver.setPhone("+91 98450 11223");
        driver.setVehicleModel("Tata Nexon EV LR");
        driver.setVehiclePlate("KA-05-MN-8842");
        driver = userRepo.save(driver);

        ChargerStation s1 = new ChargerStation();
        s1.setName("Villa 14 Smart Wallbox");
        s1.setHostName(host.getFullName());
        s1.setHostPhone(host.getPhone());
        s1.setAddress("14, 7th Main, Indiranagar 2nd Stage");
        s1.setCity("Bengaluru");
        s1.setLatitude(12.9784);
        s1.setLongitude(77.6408);
        s1.setCategory(StationCategory.P2P_HOME);
        s1.setConnectorType("Type 2 AC");
        s1.setPowerKw(7.4);
        s1.setPricePerHour(BigDecimal.valueOf(140.00));
        s1.setPricePerKwh(BigDecimal.valueOf(16.00));
        s1.setTotalBays(1);
        s1.setOnline(true);
        s1.setApprovalStatus(StationApprovalStatus.APPROVED);
        s1.setAverageRating(4.9);
        s1.setReviewCount(1);
        s1.setHost(host);
        s1 = stationRepo.save(s1);

        ChargerStation s2 = new ChargerStation();
        s2.setName("BESCOM EV Public Plaza - Cubbon Park");
        s2.setHostName("BESCOM State EV Grid");
        s2.setHostPhone("1912");
        s2.setAddress("Kasturba Road, Near Cubbon Park Metro");
        s2.setCity("Bengaluru");
        s2.setLatitude(12.9763);
        s2.setLongitude(77.5929);
        s2.setCategory(StationCategory.GOVT_PUBLIC);
        s2.setConnectorType("CCS2 DC Fast");
        s2.setPowerKw(50.0);
        s2.setPricePerHour(BigDecimal.valueOf(420.00));
        s2.setPricePerKwh(BigDecimal.valueOf(19.50));
        s2.setTotalBays(4);
        s2.setOnline(true);
        s2.setApprovalStatus(StationApprovalStatus.APPROVED);
        s2.setAverageRating(4.7);
        s2.setReviewCount(0);
        s2.setHost(host);
        stationRepo.save(s2);

        ChargerStation s3 = new ChargerStation();
        s3.setName("Orion Tech Park HyperCharge Hub");
        s3.setHostName("Zeon Commercial Mobility");
        s3.setHostPhone("+91 80 6677 2211");
        s3.setAddress("Dr. Rajkumar Road, Rajajinagar");
        s3.setCity("Bengaluru");
        s3.setLatitude(13.0110);
        s3.setLongitude(77.5550);
        s3.setCategory(StationCategory.PRIVATE_COMMERCIAL);
        s3.setConnectorType("CCS2 60kW DC");
        s3.setPowerKw(60.0);
        s3.setPricePerHour(BigDecimal.valueOf(540.00));
        s3.setPricePerKwh(BigDecimal.valueOf(22.00));
        s3.setTotalBays(6);
        s3.setOnline(true);
        s3.setApprovalStatus(StationApprovalStatus.APPROVED);
        s3.setAverageRating(4.8);
        s3.setReviewCount(0);
        s3.setHost(host);
        stationRepo.save(s3);

        Review r1 = new Review();
        r1.setCharger(s1);
        r1.setDriverId(driver.getId());
        r1.setDriverName(driver.getFullName());
        r1.setVehicleModel(driver.getVehicleModel());
        r1.setRating(5);
        r1.setComment("Covered driveway with steady 7.4kW output and instant PIN unlock.");
        reviewRepo.save(r1);
    }
}
