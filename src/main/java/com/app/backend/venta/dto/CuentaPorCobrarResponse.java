package com.app.backend.venta.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class CuentaPorCobrarResponse {
    private Long id;
    private Integer idVenta;
    private Integer idCliente;
    private String cliente;
    private BigDecimal montoTotal;
    private BigDecimal montoPagado;
    private BigDecimal saldo;
    private String estatus;
    private LocalDate fechaVencimiento;
    private LocalDateTime fechaCreacion;
    private Boolean vencida;
}