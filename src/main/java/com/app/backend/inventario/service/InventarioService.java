package com.app.backend.inventario.service;

import com.app.backend.compra.dto.OrdenCompraResponse;
import com.app.backend.compra.entity.DetalleOrdenCompra;
import com.app.backend.compra.entity.OrdenCompra;
import com.app.backend.compra.repository.OrdenCompraRepository;
import com.app.backend.compra.service.OrdenCompraService;
import com.app.backend.inventario.dto.EntradaInventarioRequest;
import com.app.backend.inventario.dto.EntradaInventarioResponse;
import com.app.backend.inventario.dto.StockProductoResponse;
import com.app.backend.producto.entity.StockVariante;
import com.app.backend.producto.repository.StockVarianteRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioService {

        private final OrdenCompraRepository ordenCompraRepository;
        private final OrdenCompraService ordenCompraService;
        private final StockVarianteRepository stockVarianteRepository;
        private final EntityManager entityManager;

        @Transactional(readOnly = true)
        public List<OrdenCompraResponse> listarOrdenesConfirmadas() {
                return ordenCompraRepository.findByEstatus("confirmada")
                                .stream()
                                .map(ordenCompraService::toResponsePublic)
                                .toList();
        }

        @Transactional
        public EntradaInventarioResponse registrarEntrada(
                        EntradaInventarioRequest request, Integer idUsuario) {

                OrdenCompra orden = ordenCompraRepository.findByIdWithDetalles(
                                request.getIdOrdenCompra())
                                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

                if (!"confirmada".equals(orden.getEstatus())) {
                        throw new IllegalArgumentException(
                                        "Solo se pueden recibir órdenes confirmadas. " +
                                                        "Estatus actual: " + orden.getEstatus());
                }

                List<EntradaInventarioResponse.MovimientoResponse> movimientos = new ArrayList<>();

                for (DetalleOrdenCompra detalle : orden.getDetalles()) {
                        Integer idVariante = detalle.getVariante().getId();

                        // Stock antes
                        Integer stockAntes = stockVarianteRepository
                                        .findById(idVariante)
                                        .map(StockVariante::getCantidad)
                                        .orElse(0);

                        // Insertar movimiento — el trigger actualiza stock automáticamente
                        ordenCompraService.insertarMovimientoEntrada(detalle, idUsuario);

                        // Flush para que el trigger actualice antes de leer
                        entityManager.flush();

                        // Stock después
                        entityManager.refresh(
                                        stockVarianteRepository.findById(idVariante)
                                                        .orElseThrow());
                        Integer stockDespues = stockVarianteRepository
                                        .findById(idVariante)
                                        .map(StockVariante::getCantidad)
                                        .orElse(0);

                        movimientos.add(
                                        EntradaInventarioResponse.MovimientoResponse.builder()
                                                        .sku(detalle.getVariante().getSku())
                                                        .producto(detalle.getVariante().getProducto().getNombre())
                                                        .variante(
                                                                        detalle.getVariante().getAtributosMap()
                                                                                        .entrySet().stream()
                                                                                        .map(e -> e.getKey() + ": "
                                                                                                        + e.getValue())
                                                                                        .collect(java.util.stream.Collectors
                                                                                                        .joining(" / ")))
                                                        .cantidadAnterior(stockAntes)
                                                        .cantidadAgregada(detalle.getCantidad())
                                                        .cantidadNueva(stockDespues)
                                                        .build());
                }

                // Marcar la orden como recibida
                ordenCompraService.marcarComoRecibida(orden.getId());

                return EntradaInventarioResponse.builder()
                                .idOrdenCompra(orden.getId())
                                .proveedor(orden.getProveedor().getNombre())
                                .fecha(LocalDateTime.now())
                                .movimientos(movimientos)
                                .build();
        }

        @Transactional(readOnly = true)
        @SuppressWarnings("unchecked")
        public List<StockProductoResponse> consultarStock() {
                List<Object[]> rows = entityManager.createNativeQuery("""
                                SELECT
                                    p.id                                         AS id_producto,
                                    p.nombre,
                                    p.descripcion,
                                    tp.nombre                                    AS tipo,
                                    MIN(COALESCE(pv.imagen_url, p.imagen_url))  AS imagen_url,
                                    MAX(COALESCE(pv.precio_venta, p.precio_venta, 0)) AS precio_venta,
                                    MAX(COALESCE(pv.precio_compra, p.precio_compra))  AS precio_compra,
                                    SUM(COALESCE(sv.cantidad, 0))               AS stock_total,
                                    COUNT(pv.id)                                AS total_variantes,
                                    JSON_AGG(
                                        JSON_BUILD_OBJECT(
                                            'idVariante', pv.id,
                                            'sku',        pv.sku,
                                            'atributos',  COALESCE(
                                                (SELECT json_object_agg(va.nombre, va.valor)
                                                 FROM variante_atributo va
                                                 WHERE va.id_variante = pv.id),
                                                '{}'::json
                                            ),
                                            'imagenUrl',  COALESCE(pv.imagen_url, p.imagen_url),
                                            'stock',      COALESCE(sv.cantidad, 0)
                                        )
                                    )                                           AS variantes
                                FROM producto_variante pv
                                JOIN producto p         ON p.id  = pv.id_producto
                                JOIN tipo_producto tp   ON tp.id = p.id_tipo_producto
                                LEFT JOIN stock_variante sv ON sv.id_variante = pv.id
                                WHERE p.activo  = true
                                  AND pv.activo = true
                                GROUP BY p.id, p.nombre, p.descripcion, tp.nombre
                                ORDER BY p.nombre
                                """).getResultList();

                return rows.stream().map(this::rowToStockResponse).toList();
        }

        @SuppressWarnings("unchecked")
        private StockProductoResponse rowToStockResponse(Object[] row) {

                return StockProductoResponse.builder()
                                .idProducto(((Number) row[0]).intValue())
                                .nombre((String) row[1])
                                .descripcion((String) row[2])
                                .tipo((String) row[3])
                                .imagenUrl((String) row[4])
                                .precioVenta(row[5] != null
                                                ? new BigDecimal(row[5].toString())
                                                : null)
                                .precioCompra(row[6] != null
                                                ? new BigDecimal(row[6].toString())
                                                : null)
                                .stockTotal(row[7] != null
                                                ? ((Number) row[7]).intValue()
                                                : 0)
                                .totalVariantes(row[8] != null
                                                ? ((Number) row[8]).intValue()
                                                : 0)
                                .variantes(parseVariantes(row[9] != null
                                                ? row[9].toString()
                                                : "[]"))
                                .build();
        }

        private List<StockProductoResponse.StockVarianteResponse> parseVariantes(String json) {
                try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        List<java.util.Map<String, Object>> lista = mapper.readValue(json,
                                        new com.fasterxml.jackson.core.type.TypeReference<>() {
                                        });

                        return lista.stream().map(m -> {
                                // atributos viene como objeto JSON anidado
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, String> atributos = m.get("atributos") instanceof java.util.Map
                                                ? (java.util.Map<String, String>) m.get("atributos")
                                                : java.util.Map.of();

                                return StockProductoResponse.StockVarianteResponse.builder()
                                                .idVariante(m.get("idVariante") != null
                                                                ? ((Number) m.get("idVariante")).intValue()
                                                                : null)
                                                .sku((String) m.get("sku"))
                                                .atributos(atributos) // ← mapa flexible
                                                .imagenUrl((String) m.get("imagenUrl"))
                                                .stock(m.get("stock") != null
                                                                ? ((Number) m.get("stock")).intValue()
                                                                : 0)
                                                .build();
                        }).toList();
                } catch (Exception e) {
                        return List.of();
                }
        }
}
