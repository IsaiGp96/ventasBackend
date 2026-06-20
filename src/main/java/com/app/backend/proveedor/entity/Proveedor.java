package com.app.backend.proveedor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proveedor")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 50)
    private String telefono;

    @Column(length = 150)
    private String correo;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Column(length = 20)
    private String rfc;

    @Column(length = 10)
    private String prefijo;
}
