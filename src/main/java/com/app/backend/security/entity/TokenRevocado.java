package com.app.backend.security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_revocado")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRevocado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", unique = true, nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "fecha_revocacion", nullable = false)
    private LocalDateTime fechaRevocacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;
}