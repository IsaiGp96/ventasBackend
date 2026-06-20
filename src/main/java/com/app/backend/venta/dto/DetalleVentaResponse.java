package com.app.backend.venta.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class DetalleVentaResponse {
    private Integer id;
    private Integer idVariante;
    private String sku;
    private String producto;
    private Map<String, String> atributos; // ← reemplaza genero/talla/color/codigoHex
    private String imagenUrl;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}