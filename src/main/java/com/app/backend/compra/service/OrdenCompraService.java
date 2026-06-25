package com.app.backend.compra.service;

import com.app.backend.compra.dto.*;
import com.app.backend.compra.entity.*;
import com.app.backend.compra.repository.*;
import com.app.backend.producto.entity.Producto;
import com.app.backend.producto.entity.ProductoVariante;
import com.app.backend.producto.repository.ProductoRepository;
import com.app.backend.producto.repository.ProductoVarianteRepository;
import com.app.backend.proveedor.entity.Proveedor;
import com.app.backend.proveedor.repository.ProveedorRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final DetalleOrdenCompraRepository detalleRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final EntityManager entityManager;
    private final ProductoVarianteRepository varianteRepository;

    // ─── Listar ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<OrdenCompraResponse> listar() {
        return ordenCompraRepository.findAllByOrderByFechaDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrdenCompraResponse obtener(Integer id) {
        return toResponse(ordenCompraRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada")));
    }

    // ─── Crear ────────────────────────────────────────────────────────────────

    @Transactional
    public OrdenCompraResponse crear(OrdenCompraRequest request) {
        Proveedor proveedor = proveedorRepository.findById(request.getIdProveedor())
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));

        validarRequest(request);

        boolean esLote = Boolean.TRUE.equals(request.getEsLote());
        BigDecimal flete = request.getFlete() != null ? request.getFlete() : BigDecimal.ZERO;

        BigDecimal costoPorPieza = calcularCostoPorPieza(esLote, request, flete);

        OrdenCompra orden = ordenCompraRepository.save(
                OrdenCompra.builder()
                        .proveedor(proveedor)
                        .estatus("borrador")
                        .flete(flete)
                        .esLote(esLote)
                        .costoLote(request.getCostoLote())
                        .piezasLote(request.getPiezasLote())
                        .build());

        guardarDetalles(orden, request.getDetalles(), esLote, costoPorPieza);
        return obtener(orden.getId());
    }

    // ─── Confirmar ────────────────────────────────────────────────────────────

    @Transactional
    public OrdenCompraResponse confirmarOrden(Integer id) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        if (!"borrador".equals(orden.getEstatus())) {
            throw new IllegalArgumentException("Solo se pueden confirmar órdenes en borrador");
        }
        orden.setEstatus("confirmada");
        return toResponse(ordenCompraRepository.save(orden));
    }

    // ─── Cancelar ─────────────────────────────────────────────────────────────

    @Transactional
    public OrdenCompraResponse cancelarOrden(Integer id) {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        if (!"borrador".equals(orden.getEstatus())
                && !"confirmada".equals(orden.getEstatus())) {
            throw new IllegalArgumentException(
                    "Solo se pueden cancelar órdenes en borrador o confirmadas");
        }
        orden.setEstatus("cancelada");
        return toResponse(ordenCompraRepository.save(orden));
    }

    // ─── Eliminar borrador ────────────────────────────────────────────────────

    @Transactional
    public void eliminarBorrador(Integer id) {
        OrdenCompra orden = ordenCompraRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        if (!"borrador".equals(orden.getEstatus())) {
            throw new IllegalArgumentException("Solo se pueden eliminar órdenes en borrador");
        }
        ordenCompraRepository.delete(orden);
    }

    // ─── Editar borrador ──────────────────────────────────────────────────────

    @Transactional
    public OrdenCompraResponse actualizarBorrador(Integer id, OrdenCompraRequest request) {
        OrdenCompra orden = ordenCompraRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        if (!"borrador".equals(orden.getEstatus())) {
            throw new IllegalArgumentException("Solo se pueden editar órdenes en borrador");
        }

        validarRequest(request);

        Proveedor proveedor = proveedorRepository.findById(request.getIdProveedor())
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));

        boolean esLote = Boolean.TRUE.equals(request.getEsLote());
        BigDecimal flete = request.getFlete() != null ? request.getFlete() : BigDecimal.ZERO;
        BigDecimal costoPorPieza = calcularCostoPorPieza(esLote, request, flete);

        // DELETE nativo garantiza que los registros se eliminan de BD inmediatamente
        entityManager.createNativeQuery(
                "DELETE FROM detalle_orden_compra WHERE id_orden_compra = :idOrden")
                .setParameter("idOrden", id)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // Recargar la orden limpia desde BD (sin los detalles eliminados)
        orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        orden.setProveedor(proveedor);
        orden.setFlete(flete);
        orden.setEsLote(esLote);
        orden.setCostoLote(request.getCostoLote());
        orden.setPiezasLote(request.getPiezasLote());
        ordenCompraRepository.save(orden);

        guardarDetalles(orden, request.getDetalles(), esLote, costoPorPieza);

        return obtener(id);
    }

    // ─── Recibir (llamado desde InventarioService) ────────────────────────────

    @Transactional
    public OrdenCompraResponse marcarComoRecibida(Integer id) {
        OrdenCompra orden = ordenCompraRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        if (!"confirmada".equals(orden.getEstatus())) {
            throw new IllegalArgumentException("Solo se pueden recibir órdenes confirmadas");
        }

        orden.setEstatus("recibida");
        ordenCompraRepository.save(orden);

        for (DetalleOrdenCompra detalle : orden.getDetalles()) {
            ProductoVariante variante = detalle.getVariante();
            BigDecimal costo = detalle.getCostoUnitario();
            if (costo == null) continue;

            variante.setPrecioCompra(costo);

            BigDecimal pctMargen   = variante.getPctMargen() != null
                    ? variante.getPctMargen() : new BigDecimal("30");
            BigDecimal pctComision = variante.getPctComision() != null
                    ? variante.getPctComision() : BigDecimal.ZERO;

            BigDecimal precioVenta = calcularPrecioVenta(costo, pctMargen, pctComision);
            variante.setPrecioVenta(precioVenta);
            varianteRepository.save(variante);

            // Actualizar precio_compra del producto como referencia (último costo)
            Producto producto = variante.getProducto();
            producto.setPrecioCompra(costo);
            productoRepository.save(producto);
        }

        return obtener(id);
    }

    private BigDecimal calcularPrecioVenta(
            BigDecimal costo, BigDecimal pctMargen, BigDecimal pctComision) {

        BigDecimal divisorMargen = BigDecimal.ONE.subtract(
                pctMargen.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal precioConMargen = costo.divide(divisorMargen, 4, RoundingMode.HALF_UP);

        if (pctComision.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal divisorComision = BigDecimal.ONE.subtract(
                    pctComision.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            return precioConMargen.divide(divisorComision, 2, RoundingMode.HALF_UP);
        }
        return precioConMargen.setScale(2, RoundingMode.HALF_UP);
    }

    // Para uso desde InventarioService
    public OrdenCompraResponse toResponsePublic(OrdenCompra o) {
        return toResponse(o);
    }

    // ─── FIX: usuario real en lugar de id hardcodeado ─────────────────────────
    // Recibe idUsuario desde InventarioService (el usuario autenticado que hace la recepción)

    void insertarMovimientoEntrada(DetalleOrdenCompra detalle, Integer idUsuario) {
        entityManager.createNativeQuery("""
                INSERT INTO movimiento_inventario
                    (id_variante, cantidad_delta, tipo,
                     id_detalle_orden_compra, id_usuario, nota)
                VALUES
                    (:idVariante, :cantidad, 'ENTRADA',
                     :idDetalle, :idUsuario, 'Recepción de orden de compra')
                """)
                .setParameter("idVariante", detalle.getVariante().getId())
                .setParameter("cantidad", detalle.getCantidad())
                .setParameter("idDetalle", detalle.getId())
                .setParameter("idUsuario", idUsuario)   // ← usuario real, no hardcodeado
                .executeUpdate();
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private void validarRequest(OrdenCompraRequest request) {
        if (request.getFlete() != null
                && request.getFlete().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El flete no puede ser negativo");
        }
        if (request.getCostoLote() != null
                && request.getCostoLote().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El costo del lote no puede ser negativo");
        }
        for (DetalleOrdenRequest dr : request.getDetalles()) {
            if (dr.getCantidad() == null || dr.getCantidad() < 1) {
                throw new IllegalArgumentException(
                        "La cantidad por línea debe ser mayor a 0");
            }
            if (dr.getCostoUnitario() != null
                    && dr.getCostoUnitario().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El costo unitario no puede ser negativo");
            }
        }
        if (Boolean.TRUE.equals(request.getEsLote())
                && request.getPiezasLote() != null) {
            int sumaLineas = request.getDetalles().stream()
                    .mapToInt(d -> d.getCantidad() != null ? d.getCantidad() : 0)
                    .sum();
            if (sumaLineas != request.getPiezasLote()) {
                throw new IllegalArgumentException(
                        "La suma de piezas (" + sumaLineas +
                                ") no coincide con el total del lote (" +
                                request.getPiezasLote() + ")");
            }
        }
    }

    private BigDecimal calcularCostoPorPieza(boolean esLote,
            OrdenCompraRequest request, BigDecimal flete) {
        if (!esLote) return null;
        if (request.getCostoLote() == null || request.getPiezasLote() == null
                || request.getPiezasLote() == 0) {
            throw new IllegalArgumentException(
                    "El modo lote requiere costo total y número de piezas");
        }
        return request.getCostoLote()
                .add(flete)
                .divide(BigDecimal.valueOf(request.getPiezasLote()),
                        4, RoundingMode.HALF_UP);
    }

    private void guardarDetalles(OrdenCompra orden,
            List<DetalleOrdenRequest> detalles,
            boolean esLote,
            BigDecimal costoPorPieza) {

        for (DetalleOrdenRequest dr : detalles) {
            BigDecimal costoLinea = esLote ? costoPorPieza : dr.getCostoUnitario();

            if (costoLinea == null) {
                throw new IllegalArgumentException(
                        "El costo unitario es obligatorio en modo normal");
            }

            ProductoVariante variante = varianteRepository.findById(dr.getIdVariante())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Variante no encontrada: " + dr.getIdVariante()));

            detalleRepository.save(
                    DetalleOrdenCompra.builder()
                            .ordenCompra(orden)
                            .variante(variante)
                            .cantidad(dr.getCantidad())
                            .costoUnitario(costoLinea.setScale(2, RoundingMode.HALF_UP))
                            .build());
        }
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    public OrdenCompraResponse toResponse(OrdenCompra o) {
        List<DetalleOrdenResponse> detalles = o.getDetalles() == null
                ? List.of()
                : o.getDetalles().stream().map(this::toDetalleResponse).toList();

        BigDecimal total = detalles.stream()
                .map(d -> d.getCostoUnitario().multiply(
                        BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costoPorPieza = null;
        if (Boolean.TRUE.equals(o.getEsLote())
                && o.getCostoLote() != null
                && o.getPiezasLote() != null
                && o.getPiezasLote() > 0) {
            costoPorPieza = o.getCostoLote()
                    .add(o.getFlete() != null ? o.getFlete() : BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(o.getPiezasLote()), 2, RoundingMode.HALF_UP);
        }

        return OrdenCompraResponse.builder()
                .id(o.getId())
                .proveedor(o.getProveedor().getNombre())
                .idProveedor(o.getProveedor().getId())
                .fecha(o.getFecha())
                .estatus(o.getEstatus())
                .detalles(detalles)
                .total(total)
                .flete(o.getFlete())
                .esLote(o.getEsLote())
                .costoLote(o.getCostoLote())
                .piezasLote(o.getPiezasLote())
                .costoPorPieza(costoPorPieza)
                .build();
    }

    private DetalleOrdenResponse toDetalleResponse(DetalleOrdenCompra d) {
        ProductoVariante v = d.getVariante();
        Map<String, String> atributos = v.getAtributosMap();

        BigDecimal subtotal = d.getCostoUnitario()
                .multiply(BigDecimal.valueOf(d.getCantidad()));

        return DetalleOrdenResponse.builder()
                .id(d.getId())
                .idVariante(v.getId())
                .sku(v.getSku())
                .producto(v.getProducto().getNombre())
                .atributos(atributos)
                .cantidad(d.getCantidad())
                .costoUnitario(d.getCostoUnitario())
                .subtotal(subtotal)
                .build();
    }
}