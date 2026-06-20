package com.app.backend.venta.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class VentaResponse {
    private Integer id;
    private Integer idCliente;
    private String cliente;
    private String usuario;
    private LocalDateTime fecha;
    private BigDecimal total;
    private BigDecimal impuestos;
    private BigDecimal descuento;
    private String metodoPago;
    private String estatus;
    private String notas;
    private List<DetalleVentaResponse> detalles;
    private Boolean tieneCxc;
}