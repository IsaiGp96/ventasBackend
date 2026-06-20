package com.app.backend.security.repository;

import com.app.backend.security.entity.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, Long> {

    boolean existsByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM TokenRevocado t WHERE t.fechaExpiracion < :ahora")
    void eliminarExpirados(LocalDateTime ahora);
}