package com.app.backend.compra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrdenCompraRequest {

    @NotNull(message = "El proveedor es obligatorio")
    private Integer idProveedor;

    @NotEmpty(message = "Debe agregar al menos un producto")
    @Valid
    private List<DetalleOrdenRequest> detalles;

    private Boolean esLote;
    private BigDecimal costoLote;
    private Integer piezasLote;
    private BigDecimal flete;
}
