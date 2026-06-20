package com.app.backend.dashboard.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {

    // ─── KPIs del período ─────────────────────────────────────────────────
    private KpiVentas ventasHoy;
    private KpiVentas ventasMes;
    private KpiVentas ventasMesAnterior;

    private BigDecimal gastosMes;
    private BigDecimal utilidadMes; // ventasMes - gastosMes - costoMercancia

    // ─── Alertas operativas ───────────────────────────────────────────────
    private Integer stockCritico; // variantes con stock <= 5
    private Integer cxcPendientes; // cuentas por cobrar sin saldar
    private BigDecimal montoCxcPendiente;
    private Integer comisionesPendientes;
    private BigDecimal montoComisionesPendiente;

    // ─── Gráficas ─────────────────────────────────────────────────────────
    private List<PuntoGrafica> ventasPorDia; // últimos 30 días
    private List<PuntoGrafica> gastosPorCategoria; // mes actual

    // ─── Últimas ventas ───────────────────────────────────────────────────
    private List<VentaResumen> ultimasVentas;

    // ─── Stock crítico detalle ────────────────────────────────────────────
    private List<VarianteCritica> variantesCriticas;

    @Data
    @Builder
    public static class KpiVentas {
        private BigDecimal total;
        private Long cantidad;
        private BigDecimal ticket; // promedio por venta
        private BigDecimal variacion; // % vs período anterior (null si no aplica)
    }

    @Data
    @Builder
    public static class PuntoGrafica {
        private String etiqueta; // "2026-05-20" o "Empaque"
        private BigDecimal valor;
        private Long cantidad;
    }

    @Data
    @Builder
    public static class VentaResumen {
        private Integer id;
        private String cliente;
        private BigDecimal total;
        private String metodoPago;
        private String fecha;
    }

    @Data
    @Builder
    public static class VarianteCritica {
        private String sku;
        private String producto;
        private String variante;
        private Integer stock;
    }
}