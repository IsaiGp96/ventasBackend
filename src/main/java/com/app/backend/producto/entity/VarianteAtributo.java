package com.app.backend.producto.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variante_atributo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VarianteAtributo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_variante", nullable = false)
    private ProductoVariante variante;

    @Column(nullable = false, length = 50)
    private String nombre; // "genero", "talla", "color", "tipo", "diseño"

    @Column(nullable = false, length = 100)
    private String valor; // "Hombre", "M", "Básico"
}