package com.app.backend.compra.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrdenCompraResponse {
    private Integer id;
    private String proveedor;
    private Integer idProveedor;
    private LocalDateTime fecha;
    private String estatus;
    private List<DetalleOrdenResponse> detalles;
    private BigDecimal total;
    private Boolean esLote;
    private BigDecimal costoLote;
    private Integer piezasLote;
    private BigDecimal flete;
    private BigDecimal costoPorPieza; // calculado: (costoLote + flete) / piezasLote
}
