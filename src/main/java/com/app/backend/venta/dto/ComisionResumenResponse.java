package com.app.backend.venta.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ComisionResumenResponse {
    private Long id;
    private Integer idVenta;
    private String usuario;
    private String periodo;
    private BigDecimal montoComision;
    private String estatus;
    private LocalDateTime fechaPago;
}