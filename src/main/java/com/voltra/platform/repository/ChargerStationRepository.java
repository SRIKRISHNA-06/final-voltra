package com.voltra.platform.repository;

import com.voltra.platform.model.ChargerStation;
import com.voltra.platform.model.StationApprovalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChargerStationRepository extends JpaRepository<ChargerStation, Long> {
    List<ChargerStation> findByHostIdOrderByIdDesc(Long hostId);
    List<ChargerStation> findByApprovalStatusOrderByIdDesc(StationApprovalStatus status);

    @Query("SELECT c FROM ChargerStation c WHERE c.approvalStatus = :status AND (" +
           "LOWER(c.city) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.address) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<ChargerStation> searchApprovedStations(@Param("q") String query,
                                                @Param("status") StationApprovalStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChargerStation c WHERE c.id = :id")
    Optional<ChargerStation> findByIdWithLock(@Param("id") Long id);
}
