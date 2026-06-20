package com.app.backend.producto.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductoResponse {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String tipoProducto;
    private String proveedor;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private Boolean activo;
    private List<VarianteResponse> variantes;
    private String imagenUrl;
    private Integer idTipoProducto;
    private Integer idProveedor;
    private Integer idCategoria;
    private String categoria;
    private String categoriaSlug;

    @Data
    @Builder
    public static class VarianteResponse {
        private Integer id;
        private String sku;
        private String codigoBarras;
        private Map<String, String> atributos;
        private BigDecimal pctMargen;
        private BigDecimal pctComision;
        private BigDecimal precioCompra;
        private BigDecimal precioVenta;
        private Integer stock;
        private Boolean activo;
        private String imagenUrl;
    }
}