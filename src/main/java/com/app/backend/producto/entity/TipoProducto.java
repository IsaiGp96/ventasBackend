package com.app.backend.producto.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Table(name = "tipo_producto")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private Boolean activo;
}
