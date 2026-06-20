package com.app.backend.inventario.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class StockProductoResponse {
    private Integer idProducto;
    private String nombre;
    private String descripcion;
    private String tipo;
    private String imagenUrl;
    private BigDecimal precioVenta;
    private Integer stockTotal;
    private Integer totalVariantes;
    private List<StockVarianteResponse> variantes;
    private BigDecimal precioCompra;

    @Data
    @Builder
    public static class StockVarianteResponse {
        private Integer idVariante;
        private String sku;
        private Map<String, String> atributos;
        private String imagenUrl;
        private Integer stock;
    }
}
