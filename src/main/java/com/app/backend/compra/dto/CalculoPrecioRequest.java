package com.app.backend.compra.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CalculoPrecioRequest {
    private Integer idOrdenCompra; // para calcular con flete prorrateado
    private BigDecimal costoUnitario;
    private Integer cantidad;
    private BigDecimal flete; // flete total de la OC
    private BigDecimal totalMercancia; // para prorratear
    private BigDecimal pctComision;
    private BigDecimal pctIva;
    private Boolean exentoIva;
    private BigDecimal pctMargen;
    private Boolean usarMargenReal; // true = margen sobre precio, false = markup sobre costo

}