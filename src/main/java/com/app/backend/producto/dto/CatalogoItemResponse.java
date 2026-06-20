package com.app.backend.producto.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CatalogoItemResponse {
    private Integer idVariante;
    private String sku;
    private String codigoBarras;
    private Integer idProducto;
    private String nombre;
    private String descripcion;
    private String tipo;
    private String genero;
    private String talla;
    private String color;
    private String codigoHex;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private Integer stock;
}
