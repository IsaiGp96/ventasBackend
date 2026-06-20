package com.app.backend.compra.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class DetalleOrdenRequest {

    @NotNull(message = "La variante es obligatoria")
    private Integer idVariante;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    // Opcional en modo lote — obligatorio en modo normal
    @DecimalMin(value = "0.0", message = "El costo no puede ser negativo")
    private BigDecimal costoUnitario;
}