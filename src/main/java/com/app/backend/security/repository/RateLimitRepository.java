package com.app.backend.security.repository;

import com.app.backend.security.entity.RateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RateLimitRepository extends JpaRepository<RateLimit, Long> {

    Optional<RateLimit> findByIpAndEndpoint(String ip, String endpoint);

    @Modifying
    @Query("DELETE FROM RateLimit r WHERE r.ventanaInicio < :antes")
    void eliminarVentanasAntiguas(LocalDateTime antes);
}