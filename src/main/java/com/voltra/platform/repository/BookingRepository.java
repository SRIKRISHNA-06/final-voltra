package com.voltra.platform.repository;

import com.voltra.platform.model.Booking;
import com.voltra.platform.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByDriverIdOrderByStartTimeDesc(Long driverId);
    List<Booking> findByChargerHostIdOrderByStartTimeDesc(Long hostId);
    List<Booking> findAllByOrderByCreatedAtDesc();
    Optional<Booking> findByRazorpayOrderId(String razorpayOrderId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.charger.id = :chargerId " +
           "AND b.status IN :activeStatuses " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    long countOverlappingBookings(@Param("chargerId") Long chargerId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime,
                                  @Param("activeStatuses") List<BookingStatus> activeStatuses);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime cutoff);
}
