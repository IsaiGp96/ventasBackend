package com.app.backend.compra.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CalculoPrecioResponse {
    private BigDecimal costoUnitario;
    private BigDecimal fletePorPieza;
    private BigDecimal costoReal;
    private BigDecimal factorComision;
    private BigDecimal factorIva;
    private BigDecimal factorMargen;
    private BigDecimal precioSugerido;
    private BigDecimal margenPesos; // ganancia en $ por pieza
    private BigDecimal pctMargenReal; // margen real sobre el precio final
}