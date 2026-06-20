package com.app.backend.venta.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DetalleVentaRequest {

    @NotNull(message = "La variante es obligatoria")
    private Integer idVariante;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    // Si no viene, se usa el precio_venta del producto base
    private BigDecimal precioUnitario;
}