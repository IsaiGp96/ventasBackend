package com.app.backend.dashboard.service;

import com.app.backend.dashboard.dto.DashboardResponse;
import com.app.backend.gasto.repository.GastoRepository;
import com.app.backend.venta.repository.CuentaPorCobrarRepository;
import com.app.backend.venta.repository.ComisionVentaRepository;
import com.app.backend.venta.repository.VentaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
// import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    // private final VentaRepository ventaRepository;
    private final GastoRepository gastoRepository;
    // private final CuentaPorCobrarRepository cxcRepository;
    private final ComisionVentaRepository comisionRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public DashboardResponse obtener(Integer idUsuario, boolean verTodo) {

        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1);
        LocalDate finMesAnterior = inicioMes.minusDays(1);
        LocalDate hace30dias = hoy.minusDays(29);

        // ─── KPIs de ventas ───────────────────────────────────────────────
        DashboardResponse.KpiVentas ventasHoy = calcularKpiVentas(
                hoy, hoy, idUsuario, verTodo);
        DashboardResponse.KpiVentas ventasMes = calcularKpiVentas(
                inicioMes, hoy, idUsuario, verTodo);
        DashboardResponse.KpiVentas ventasMesAnterior = calcularKpiVentas(
                inicioMesAnterior, finMesAnterior, idUsuario, verTodo);

        BigDecimal variacion = null;
        if (ventasMesAnterior.getTotal().compareTo(BigDecimal.ZERO) > 0) {
            variacion = ventasMes.getTotal()
                    .subtract(ventasMesAnterior.getTotal())
                    .divide(ventasMesAnterior.getTotal(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }
        ventasMes.setVariacion(variacion);

        // ─── Gastos del mes ───────────────────────────────────────────────
        BigDecimal gastosMes = verTodo
                ? gastoRepository.findByPeriodo(inicioMes, hoy).stream()
                        .map(g -> g.getMonto()).reduce(BigDecimal.ZERO, BigDecimal::add)
                : gastoRepository.findByPeriodoUsuario(inicioMes, hoy, idUsuario).stream()
                        .map(g -> g.getMonto()).reduce(BigDecimal.ZERO, BigDecimal::add);

        // ─── Costo de mercancía vendida (mes) ─────────────────────────────
        BigDecimal costoMercancia = obtenerCostoMercancia(inicioMes, hoy);

        // Utilidad = ventas - gastos - costo mercancía
        BigDecimal utilidad = verTodo
                ? ventasMes.getTotal().subtract(gastosMes).subtract(costoMercancia)
                : null;

        // ─── Comisiones pendientes ────────────────────────────────────────
        var comisiones = verTodo
                ? comisionRepository.findByEstatusOrderByFechaCreacionDesc("pendiente")
                : comisionRepository.findByUsuarioIdAndEstatusOrderByFechaCreacionDesc(
                        idUsuario, "pendiente");

        long comisionesPendientes = comisiones.size();
        BigDecimal montoComisiones = comisiones.stream()
                .map(c -> c.getMontoComision())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ─── Alertas — solo admin ─────────────────────────────────────────
        List<Object[]> stockCriticoRows = verTodo ? obtenerStockCritico() : List.of();
        Integer stockCritico = verTodo ? stockCriticoRows.size() : null;

        List<Object[]> cxcRows = verTodo ? obtenerCxcPendientes() : List.of();
        Integer cxcPendientes = verTodo ? cxcRows.size() : null;
        BigDecimal montoCxc = verTodo ? cxcRows.stream()
                .map(r -> r[2] != null ? new BigDecimal(r[2].toString()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : null;

        // ─── Gráficas ─────────────────────────────────────────────────────
        List<DashboardResponse.PuntoGrafica> ventasPorDia = obtenerVentasPorDia(hace30dias, hoy, idUsuario, verTodo);

        List<DashboardResponse.PuntoGrafica> gastosPorCategoria = verTodo ? obtenerGastosPorCategoria(inicioMes, hoy)
                : List.of();

        // ─── Últimas ventas ───────────────────────────────────────────────
        List<DashboardResponse.VentaResumen> ultimasVentas = obtenerUltimasVentas(idUsuario, verTodo);

        // ─── Variantes críticas ───────────────────────────────────────────
        List<DashboardResponse.VarianteCritica> variantesCriticas = stockCriticoRows.stream()
                .limit(10)
                .map(r -> DashboardResponse.VarianteCritica.builder()
                        .sku((String) r[0])
                        .producto((String) r[1])
                        .variante((String) r[2])
                        .stock(((Number) r[3]).intValue())
                        .build())
                .toList();

        return DashboardResponse.builder()
                .ventasHoy(ventasHoy)
                .ventasMes(ventasMes)
                .ventasMesAnterior(ventasMesAnterior)
                .gastosMes(gastosMes)
                .utilidadMes(utilidad)
                .stockCritico(stockCritico)
                .cxcPendientes(cxcPendientes)
                .montoCxcPendiente(montoCxc)
                .comisionesPendientes((int) comisionesPendientes)
                .montoComisionesPendiente(montoComisiones)
                .ventasPorDia(ventasPorDia)
                .gastosPorCategoria(gastosPorCategoria)
                .ultimasVentas(ultimasVentas)
                .variantesCriticas(variantesCriticas)
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private DashboardResponse.KpiVentas calcularKpiVentas(
            LocalDate inicio, LocalDate fin,
            Integer idUsuario, boolean verTodo) {

        String filtroUsuario = verTodo ? "" : "AND id_usuario = :idUsuario\n";

        var query = entityManager.createNativeQuery("""
                SELECT
                    COALESCE(SUM(total), 0) AS total,
                    COUNT(*) AS cantidad
                FROM venta
                WHERE estatus = 'completada'
                  AND DATE(fecha) BETWEEN :inicio AND :fin
                """ + filtroUsuario)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin);

        if (!verTodo)
            query.setParameter("idUsuario", idUsuario);

        Object[] r = (Object[]) query.getResultList().get(0);
        BigDecimal total = new BigDecimal(r[0].toString());
        long cantidad = ((Number) r[1]).longValue();
        BigDecimal ticket = cantidad > 0
                ? total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DashboardResponse.KpiVentas.builder()
                .total(total)
                .cantidad(cantidad)
                .ticket(ticket)
                .build();
    }

    private BigDecimal obtenerCostoMercancia(LocalDate inicio, LocalDate fin) {
        Object result = entityManager.createNativeQuery("""
                SELECT COALESCE(SUM(dv.cantidad * p.precio_compra), 0)
                FROM detalle_venta dv
                JOIN venta v ON v.id = dv.id_venta
                JOIN producto_variante pv ON pv.id = dv.id_variante
                JOIN producto p ON p.id = pv.id_producto
                WHERE v.estatus = 'completada'
                  AND DATE(v.fecha) BETWEEN :inicio AND :fin
                  AND p.precio_compra IS NOT NULL
                """)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getSingleResult(); // ← getSingleResult en lugar de getResultList

        return result != null ? new BigDecimal(result.toString()) : BigDecimal.ZERO;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> obtenerStockCritico() {
        return entityManager.createNativeQuery("""
                         SELECT
                    pv.sku,
                    p.nombre AS producto,
                    COALESCE(
                        string_agg(va.nombre || ': ' || va.valor, ' / '
                            ORDER BY va.nombre),
                        ''
                    ) AS variante,
                    sv.cantidad AS stock
                FROM stock_variante sv
                JOIN producto_variante pv ON pv.id = sv.id_variante
                JOIN producto p ON p.id = pv.id_producto
                LEFT JOIN variante_atributo va ON va.id_variante = pv.id
                WHERE sv.cantidad <= 5
                  AND pv.activo = true
                  AND p.activo = true
                GROUP BY pv.sku, p.nombre, sv.cantidad
                ORDER BY sv.cantidad ASC, p.nombre
                LIMIT 20
                        """).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> obtenerCxcPendientes() {
        return entityManager.createNativeQuery("""
                SELECT
                    cxc.id,
                    cl.nombre AS cliente,
                    cxc.saldo,
                    cxc.fecha_vencimiento
                FROM cuenta_por_cobrar cxc
                JOIN cliente cl ON cl.id = cxc.id_cliente
                WHERE cxc.estatus IN ('pendiente', 'parcial')
                ORDER BY cxc.fecha_vencimiento ASC
                """).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<DashboardResponse.PuntoGrafica> obtenerVentasPorDia(
            LocalDate inicio, LocalDate fin,
            Integer idUsuario, boolean verTodo) {

        String filtroUsuario = verTodo ? "" : "AND id_usuario = :idUsuario\n";

        var query = entityManager.createNativeQuery("""
                SELECT
                    DATE(fecha) AS dia,
                    COALESCE(SUM(total), 0) AS total,
                    COUNT(*) AS cantidad
                FROM venta
                WHERE estatus = 'completada'
                  AND DATE(fecha) BETWEEN :inicio AND :fin
                """ + filtroUsuario + """
                GROUP BY DATE(fecha)
                ORDER BY dia
                """)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin);

        if (!verTodo)
            query.setParameter("idUsuario", idUsuario);

        List<Object[]> rows = query.getResultList();

        return rows.stream().map(r -> DashboardResponse.PuntoGrafica.builder()
                .etiqueta(r[0].toString())
                .valor(new BigDecimal(r[1].toString()))
                .cantidad(((Number) r[2]).longValue())
                .build()).toList();
    }

    @SuppressWarnings("unchecked")
    private List<DashboardResponse.PuntoGrafica> obtenerGastosPorCategoria(
            LocalDate inicio, LocalDate fin) {

        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT
                    cg_grupo.nombre AS grupo,
                    COALESCE(SUM(g.monto), 0) AS total,
                    COUNT(*) AS cantidad
                FROM gasto g
                JOIN categoria_gasto cg ON cg.id = g.id_categoria
                JOIN categoria_gasto cg_sub ON cg_sub.id = cg.id_padre
                JOIN categoria_gasto cg_grupo ON cg_grupo.id = cg_sub.id_padre
                WHERE g.fecha BETWEEN :inicio AND :fin
                GROUP BY cg_grupo.nombre
                ORDER BY total DESC
                """)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .getResultList();

        return rows.stream().map(r -> DashboardResponse.PuntoGrafica.builder()
                .etiqueta((String) r[0])
                .valor(new BigDecimal(r[1].toString()))
                .cantidad(((Number) r[2]).longValue())
                .build()).toList();
    }

    @SuppressWarnings("unchecked")
    private List<DashboardResponse.VentaResumen> obtenerUltimasVentas(Integer idUsuario, boolean verTodo) {

        String filtroUsuario = verTodo ? "" : "AND v.id_usuario = :idUsuario\n";

        var query = entityManager.createNativeQuery("""
                SELECT
                    v.id,
                    COALESCE(cl.nombre, 'Público general') AS cliente,
                    v.total,
                    v.metodo_pago,
                    v.fecha
                FROM venta v
                LEFT JOIN cliente cl ON cl.id = v.id_cliente
                WHERE v.estatus = 'completada'
                """ + filtroUsuario + """
                ORDER BY v.fecha DESC
                LIMIT 5
                """);

        if (!verTodo)
            query.setParameter("idUsuario", idUsuario);
        List<Object[]> rows = query.getResultList();

        return rows.stream().map(r -> DashboardResponse.VentaResumen.builder()
                .id(((Number) r[0]).intValue())
                .cliente((String) r[1])
                .total(new BigDecimal(r[2].toString()))
                .metodoPago((String) r[3])
                .fecha(r[4].toString())
                .build()).toList();
    }
}