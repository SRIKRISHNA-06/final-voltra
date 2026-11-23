package com.voltra.platform.repository;

import com.voltra.platform.model.HostPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HostPayoutRepository extends JpaRepository<HostPayout, Long> {
    List<HostPayout> findByHostIdOrderByRequestedAtDesc(Long hostId);
    List<HostPayout> findAllByOrderByRequestedAtDesc();
}
