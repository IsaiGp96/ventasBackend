package com.app.backend.producto.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class VarianteResponse {
    private Integer id;
    private String sku;
    private String codigoBarras;
    private Map<String, String> atributos;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta; 
    private BigDecimal pctMargen;
    private BigDecimal pctComision;
    private Integer stock;
    private Boolean activo;
    private String imagenUrl;
}