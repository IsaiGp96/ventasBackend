package com.app.backend.security.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rate_limit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(nullable = false, length = 100)
    private String endpoint;

    @Column(nullable = false)
    private Integer contador;

    @Column(name = "ventana_inicio", nullable = false)
    private LocalDateTime ventanaInicio;
}