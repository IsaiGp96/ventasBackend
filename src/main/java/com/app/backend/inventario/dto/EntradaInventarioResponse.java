package com.app.backend.inventario.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EntradaInventarioResponse {
    private Integer idOrdenCompra;
    private String proveedor;
    private LocalDateTime fecha;
    private List<MovimientoResponse> movimientos;

    @Data
    @Builder
    public static class MovimientoResponse {
        private String sku;
        private String producto;
        private String variante;
        private Integer cantidadAnterior;
        private Integer cantidadAgregada;
        private Integer cantidadNueva;
    }
}