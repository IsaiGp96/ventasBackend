package com.app.backend.security.service;

import com.app.backend.security.entity.RateLimit;
import com.app.backend.security.repository.RateLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitRepository rateLimitRepository;
    private final CacheManager cacheManager;

    // Ventana de tiempo en minutos y límite de requests
    private static final int VENTANA_MINUTOS = 1;
    private static final int LIMITE_LOGIN = 5; // 5 intentos por minuto en /login
    private static final int LIMITE_GENERAL = 60; // 60 requests por minuto general

    // @Transactional
    public boolean permitir(String ip, String endpoint) {

        int limite = endpoint.contains("login") || endpoint.contains("register")
                ? LIMITE_LOGIN
                : LIMITE_GENERAL;

        String cacheKey = ip + "|" + endpoint;
        Cache cache = cacheManager.getCache("rateLimit");

        // Intentar obtener del caché
        Cache.ValueWrapper wrapper = cache != null ? cache.get(cacheKey) : null;
        RateLimitEntry entry = wrapper != null ? (RateLimitEntry) wrapper.get() : null;

        LocalDateTime ahora = LocalDateTime.now();

        if (entry == null || entry.ventanaInicio.isBefore(ahora.minusMinutes(VENTANA_MINUTOS))) {
            // Nueva ventana — crear entrada fresca
            entry = new RateLimitEntry(1, ahora);
            if (cache != null)
                cache.put(cacheKey, entry);
            persistirRateLimit(ip, endpoint, 1, ahora);
            return true;
        }

        int contador = entry.contador.incrementAndGet();
        if (cache != null)
            cache.put(cacheKey, entry);

        if (contador > limite) {
            return false;
        }

        // Persistir a BD cada 10 requests para no saturar
        if (contador % 10 == 0) {
            persistirRateLimit(ip, endpoint, contador, entry.ventanaInicio);
        }
        System.out.println(">>> RateLimit [" + ip + "] " + endpoint + " = " + contador + "/" + limite);

        return true;
    }

    @Transactional
    void persistirRateLimit(String ip, String endpoint, int contador, LocalDateTime inicio) {
        rateLimitRepository.findByIpAndEndpoint(ip, endpoint).ifPresentOrElse(
                registro -> {
                    registro.setContador(contador);
                    registro.setVentanaInicio(inicio);
                    rateLimitRepository.save(registro);
                },
                () -> rateLimitRepository.save(
                        RateLimit.builder()
                                .ip(ip).endpoint(endpoint)
                                .contador(contador).ventanaInicio(inicio)
                                .build()));
    }

    // Entrada de caché con AtomicInteger para thread-safety
    public static class RateLimitEntry implements java.io.Serializable {
        public final AtomicInteger contador;
        public final LocalDateTime ventanaInicio;

        public RateLimitEntry(int contador, LocalDateTime ventanaInicio) {
            this.contador = new AtomicInteger(contador);
            this.ventanaInicio = ventanaInicio;
        }
    }
}