package com.app.backend.security.task;

import com.app.backend.security.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LimpiezaSeguridad {

    private final TokenRevocadoRepository tokenRevocadoRepository;
    private final RateLimitRepository rateLimitRepository;
    private final IdempotenciaRepository idempotenciaRepository;
    private final CacheManager cacheManager;

    // Cada hora limpia registros expirados
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void limpiar() {
        // Limpia DB
        LocalDateTime ahora = LocalDateTime.now();
        tokenRevocadoRepository.eliminarExpirados(ahora);
        idempotenciaRepository.eliminarExpirados(ahora);
        rateLimitRepository.eliminarVentanasAntiguas(ahora.minusHours(1));

        // Limpia caché en memoria
        var cacheTokens = cacheManager.getCache("tokenRevocado");
        var cacheRate = cacheManager.getCache("rateLimit");
        if (cacheTokens != null)
            cacheTokens.clear();
        if (cacheRate != null)
            cacheRate.clear();

        System.out.println("[Limpieza] Registros de seguridad expirados eliminados");
    }
}
