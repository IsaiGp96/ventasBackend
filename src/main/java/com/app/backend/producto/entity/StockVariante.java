package com.app.backend.producto.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_variante")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockVariante {
    @Id
    @Column(name = "id_variante")
    private Integer idVariante;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id_variante")
    private ProductoVariante variante;
}
