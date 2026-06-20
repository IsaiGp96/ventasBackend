package com.app.backend.security.service;

import com.app.backend.security.entity.Idempotencia;
import com.app.backend.security.repository.IdempotenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotenciaService {

    private final IdempotenciaRepository idempotenciaRepository;

    // Cuánto tiempo guardamos la respuesta (24 horas)
    private static final int HORAS_EXPIRACION = 24;

    @Transactional(readOnly = true)
    public Optional<Idempotencia> buscar(String key) {
        return idempotenciaRepository.findByIdempotencyKey(key);
    }

    @Transactional
    public void guardar(String key, String endpoint, int status, String body) {
        // Si ya existe (race condition) no falla
        if (idempotenciaRepository.findByIdempotencyKey(key).isPresent())
            return;

        idempotenciaRepository.save(
                Idempotencia.builder()
                        .idempotencyKey(key)
                        .endpoint(endpoint)
                        .responseStatus(status)
                        .responseBody(body)
                        .fechaCreacion(LocalDateTime.now())
                        .fechaExpiracion(LocalDateTime.now().plusHours(HORAS_EXPIRACION))
                        .build());
    }
}
