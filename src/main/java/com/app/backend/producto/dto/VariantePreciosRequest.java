package com.app.backend.producto.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class VariantePreciosRequest {
    @NotNull
    @DecimalMin("0")
    @DecimalMax("99")
    private BigDecimal pctMargen;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("99")
    private BigDecimal pctComision;
}
