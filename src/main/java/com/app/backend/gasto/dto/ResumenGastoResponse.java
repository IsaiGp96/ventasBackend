package com.app.backend.gasto.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ResumenGastoResponse {

    private BigDecimal totalPeriodo;
    private BigDecimal totalFijos;
    private BigDecimal totalVariables;

    private List<ResumenCategoria> porCategoria;
    private List<ResumenMes> porMes;
    private List<GastoResponse> reembolsosPendientes;

    @Data
    @Builder
    public static class ResumenCategoria {
        private String grupo;
        private String subcategoria;
        private String categoria;
        private String tipo;
        private BigDecimal total;
        private Long cantidad;
    }

    @Data
    @Builder
    public static class ResumenMes {
        private String periodo; // "2026-05"
        private BigDecimal total;
        private Long cantidad;
    }
}
