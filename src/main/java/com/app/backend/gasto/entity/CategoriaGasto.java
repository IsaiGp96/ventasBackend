package com.app.backend.gasto.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categoria_gasto")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_padre")
    private CategoriaGasto padre;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false)
    private Integer nivel;

    @Column(nullable = false, length = 20)
    private String tipo; // fijo | variable

    @Column(nullable = false)
    private Boolean activo;
}