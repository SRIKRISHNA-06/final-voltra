package com.voltra.platform.repository;

import com.voltra.platform.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByChargerIdOrderByCreatedAtDesc(Long chargerId);
}
