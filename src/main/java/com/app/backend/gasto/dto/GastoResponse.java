package com.app.backend.gasto.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class GastoResponse {
    private Long id;
    private LocalDate fecha;
    private BigDecimal monto;
    private String descripcion;
    private Integer idProveedor;
    private String proveedor;
    private String comprobanteUrl;
    private String categoria; // nivel 3
    private String subcategoria; // nivel 2
    private String grupo; // nivel 1
    private String tipo; // fijo | variable
    private Integer idCategoria;

    private Integer idPagadoPor;
    private String pagadoPor; // nombre del empleado
    private String estatusReembolso;
    private java.time.LocalDate fechaReembolso;
    private Boolean esReembolso; // true si pagadoPor != null
    private List<GastoResponse> reembolsosPendientes;
}