package com.app.backend.venta.entity;

import com.app.backend.producto.entity.ProductoVariante;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_venta")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_variante", nullable = false)
    private ProductoVariante variante;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    // subtotal es columna calculada en BD — solo lectura
    @Column(insertable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "pct_comision", nullable = false, precision = 5, scale = 2)
    private BigDecimal pctComision;

    @Column(name = "monto_comision", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoComision;

    @Column(name = "precio_base", precision = 10, scale = 2)
    private BigDecimal precioBase;
}