package com.app.backend.security.repository;

import com.app.backend.security.entity.Idempotencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotenciaRepository extends JpaRepository<Idempotencia, Long> {

    Optional<Idempotencia> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("DELETE FROM Idempotencia i WHERE i.fechaExpiracion < :ahora")
    void eliminarExpirados(LocalDateTime ahora);
}