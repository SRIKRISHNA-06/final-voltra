package com.voltra.platform.repository;

import com.voltra.platform.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByDriverIdOrderByCreatedAtDesc(Long driverId);
    List<LedgerEntry> findByHostIdOrderByCreatedAtDesc(Long hostId);
    List<LedgerEntry> findAllByOrderByCreatedAtDesc();
}
