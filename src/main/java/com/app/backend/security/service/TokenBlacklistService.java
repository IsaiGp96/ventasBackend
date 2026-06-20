package com.app.backend.security.service;

import com.app.backend.security.entity.TokenRevocado;
import com.app.backend.security.repository.TokenRevocadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
// import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenRevocadoRepository tokenRevocadoRepository;

    @Transactional
    @CachePut(value = "tokenRevocado", key = "#result")
    public String revocar(String token, String email, LocalDateTime expiracion) {
        String hash = sha256(token);
        if (!tokenRevocadoRepository.existsByTokenHash(hash)) {
            tokenRevocadoRepository.save(
                    TokenRevocado.builder()
                            .tokenHash(hash)
                            .email(email)
                            .fechaRevocacion(LocalDateTime.now())
                            .fechaExpiracion(expiracion)
                            .build());
        }
        return hash;
    }

    @CachePut(value = "tokenRevocado", key = "#result")
    @Transactional(readOnly = true)
    public boolean estaRevocado(String tokenHash) {
        return tokenRevocadoRepository.existsByTokenHash(sha256(tokenHash));
    }

    // Guardamos el hash SHA-256, nunca el token completo
    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash)
                hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al hashear token", e);
        }
    }
}
