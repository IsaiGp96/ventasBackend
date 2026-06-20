package com.app.backend.venta.entity;

import com.app.backend.cliente.entity.Cliente;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuenta_por_cobrar")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaPorCobrar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagado;

    // saldo es columna generada en BD — solo lectura
    @Column(insertable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal saldo;

    @Column(nullable = false, length = 20)
    private String estatus;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.montoPagado == null)
            this.montoPagado = BigDecimal.ZERO;
        if (this.estatus == null)
            this.estatus = "pendiente";
    }
}