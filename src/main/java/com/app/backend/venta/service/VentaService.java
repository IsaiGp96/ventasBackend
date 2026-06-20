package com.app.backend.venta.service;

import com.app.backend.cliente.entity.Cliente;
import com.app.backend.cliente.repository.ClienteRepository;
import com.app.backend.gasto.entity.CategoriaGasto;
import com.app.backend.gasto.entity.Gasto;
import com.app.backend.gasto.repository.GastoRepository;
import com.app.backend.producto.entity.ProductoVariante;
import com.app.backend.producto.repository.ProductoVarianteRepository;
import com.app.backend.producto.repository.StockVarianteRepository;
import com.app.backend.user.entity.User;
import com.app.backend.user.repository.UserRepository;
import com.app.backend.venta.dto.*;
import com.app.backend.venta.entity.*;
import com.app.backend.venta.repository.*;
import com.app.backend.producto.repository.ProductoRepository;
// Imports adicionales
// import com.app.backend.gasto.entity.Gasto;
// import com.app.backend.gasto.entity.CategoriaGasto;
// import com.app.backend.gasto.repository.GastoRepository;
import com.app.backend.gasto.repository.CategoriaGastoRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentaService {

        private final VentaRepository ventaRepository;
        private final DetalleVentaRepository detalleRepository;
        private final CuentaPorCobrarRepository cxcRepository;
        private final PagoCxcRepository pagoCxcRepository;
        private final ClienteRepository clienteRepository;
        private final UserRepository userRepository;
        private final ProductoVarianteRepository varianteRepository;
        private final StockVarianteRepository stockRepository;
        private final ComisionVentaRepository comisionRepository;
        // private final ProductoRepository productoRepository;
        private final GastoRepository gastoRepository;
        private final CategoriaGastoRepository categoriaGastoRepository;

        // private static final Integer ID_CATEGORIA_COMISIONES = 106;
        // ─── Listar ───────────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<VentaResponse> listar(Integer idUsuario, boolean verTodas) {
                if (verTodas) {
                        return ventaRepository.findAllByOrderByFechaDesc()
                                        .stream().map(this::toResponse).toList();
                }
                return ventaRepository.findByUsuarioIdOrderByFechaDesc(idUsuario)
                                .stream().map(this::toResponse).toList();
        }

        @Transactional(readOnly = true)
        public VentaResponse obtener(Integer id) {
                return toResponse(ventaRepository.findByIdWithDetalles(id)
                                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada")));
        }

        // ─── Crear venta ──────────────────────────────────────────────────────────

        @Transactional
        public VentaResponse crear(VentaRequest request, Integer idUsuario) {
                User usuario = userRepository.findById(idUsuario)
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                Cliente cliente = null;
                if (request.getIdCliente() != null) {
                        cliente = clienteRepository.findById(request.getIdCliente())
                                        .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

                        // Validar crédito
                        if ("credito".equals(request.getMetodoPago())) {
                                if (cliente.getLimiteCredito().compareTo(BigDecimal.ZERO) == 0) {
                                        throw new IllegalArgumentException(
                                                        "Este cliente no tiene límite de crédito habilitado");
                                }
                        }
                } else if ("credito".equals(request.getMetodoPago())) {
                        throw new IllegalArgumentException(
                                        "Las ventas a crédito requieren un cliente registrado");
                }

                // Calcular total
                BigDecimal subtotalBruto = BigDecimal.ZERO;
                BigDecimal totalComision = BigDecimal.ZERO;

                List<ProductoVariante> variantes = new ArrayList<>();

                for (DetalleVentaRequest dr : request.getDetalles()) {
                        ProductoVariante variante = varianteRepository.findById(dr.getIdVariante())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Variante no encontrada: " + dr.getIdVariante()));

                        // Verificar stock
                        Integer stock = stockRepository.findById(variante.getId())
                                        .map(s -> s.getCantidad())
                                        .orElse(0);

                        if (stock < dr.getCantidad()) {
                                throw new IllegalArgumentException(
                                                "Stock insuficiente para " + variante.getSku() +
                                                                ". Disponible: " + stock + ", solicitado: "
                                                                + dr.getCantidad());
                        }

                        // Precio efectivo
                        BigDecimal precio = dr.getPrecioUnitario() != null
                                        ? dr.getPrecioUnitario()
                                        : variante.getProducto().getPrecioVenta();

                        // Totalizar venta
                        subtotalBruto = subtotalBruto.add(
                                        precio.multiply(BigDecimal.valueOf(dr.getCantidad())));
                        variantes.add(variante);
                        // Fin totalizar venta

                        BigDecimal precioCompra = variante.getPrecioCompra() != null
                                        ? variante.getPrecioCompra()
                                        : variante.getProducto().getPrecioCompra();
                        if (precioCompra != null && precioCompra.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal precioMinimo = precioCompra
                                                .multiply(new BigDecimal("1.10"))
                                                .setScale(2, RoundingMode.HALF_UP);
                                if (precio.compareTo(precioMinimo) < 0) {
                                        throw new IllegalArgumentException(
                                                        "El precio de " + variante.getSku() +
                                                                        " ($" + precio
                                                                        + ") está por debajo del mínimo permitido" +
                                                                        " ($" + precioMinimo + " = costo $"
                                                                        + precioCompra + " × 1.10)");
                                }
                        }

                }

                // Aplicar descuento
                BigDecimal pctDescuento = request.getDescuento() != null
                                ? request.getDescuento()
                                : BigDecimal.ZERO;
                BigDecimal factorDescuento = BigDecimal.ONE.subtract(
                                pctDescuento.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                BigDecimal total = subtotalBruto.multiply(factorDescuento)
                                .setScale(2, RoundingMode.HALF_UP);

                // Crear venta
                Venta venta = Venta.builder()
                                .cliente(cliente)
                                .usuario(usuario)
                                .total(total)
                                .impuestos(BigDecimal.ZERO)
                                .metodoPago(request.getMetodoPago())
                                .descuento(pctDescuento)
                                .notas(request.getNotas())
                                .estatus("completada")
                                .build();

                venta = ventaRepository.save(venta);

                // Insertar detalles — el trigger descuenta stock automáticamente
                for (int i = 0; i < request.getDetalles().size(); i++) {
                        DetalleVentaRequest dr = request.getDetalles().get(i);
                        ProductoVariante variante = variantes.get(i);

                        // precio real ingresado por el empleado
                        BigDecimal precioReal = dr.getPrecioUnitario() != null
                                        ? dr.getPrecioUnitario()
                                        : (variante.getPrecioVenta() != null
                                                        ? variante.getPrecioVenta()
                                                        : variante.getProducto().getPrecioVenta());

                        // precio base = precio_venta calculado de la variante
                        BigDecimal precioBase = variante.getPrecioVenta() != null
                                        ? variante.getPrecioVenta()
                                        : precioReal;

                        // pct_comision de la variante
                        BigDecimal pctComision = variante.getPctComision() != null
                                        ? variante.getPctComision()
                                        : BigDecimal.ZERO;

                        // comisión con lógica 50/50 en excedente
                        BigDecimal comisionUnitaria = calcularComision(
                                        precioReal, precioBase, pctComision);

                        // comisión total de la línea (× cantidad)
                        BigDecimal montoComisionLinea = comisionUnitaria
                                        .multiply(BigDecimal.valueOf(dr.getCantidad()))
                                        .setScale(2, RoundingMode.HALF_UP);

                        totalComision = totalComision.add(montoComisionLinea);

                        detalleRepository.save(
                                        DetalleVenta.builder()
                                                        .venta(venta)
                                                        .variante(variante)
                                                        .cantidad(dr.getCantidad())
                                                        .precioUnitario(precioReal)
                                                        .precioBase(precioBase)
                                                        .pctComision(pctComision)
                                                        .montoComision(montoComisionLinea)
                                                        .build());
                }

                // Generar comisión acumulada si hay monto
                if (totalComision.compareTo(BigDecimal.ZERO) > 0) {
                        String periodo = venta.getFecha()
                                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
                        comisionRepository.save(
                                        ComisionVenta.builder()
                                                        .venta(venta)
                                                        .usuario(usuario)
                                                        .periodo(periodo)
                                                        .montoComision(totalComision)
                                                        .build());
                }

                // Generar CXC si es venta a crédito
                if ("credito".equals(request.getMetodoPago()) && cliente != null) {
                        cxcRepository.save(
                                        CuentaPorCobrar.builder()
                                                        .venta(venta)
                                                        .cliente(cliente)
                                                        .montoTotal(total)
                                                        .fechaVencimiento(LocalDate.now().plusDays(30))
                                                        .build());
                }

                return obtener(venta.getId());
        }

        // ─── Cancelar venta ───────────────────────────────────────────────────────

        @Transactional
        public VentaResponse cancelar(Integer id) {
                Venta venta = ventaRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada"));

                if (!"completada".equals(venta.getEstatus())) {
                        throw new IllegalArgumentException("Solo se pueden cancelar ventas completadas");
                }

                venta.setEstatus("cancelada");
                // El ajuste de stock se haría con un movimiento de AJUSTE
                // por simplicidad en el MVP no revertimos automáticamente
                return toResponse(ventaRepository.save(venta));
        }

        // ─── CXC ──────────────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<CuentaPorCobrarResponse> listarCxcPendientes() {
                return cxcRepository.findPendientes()
                                .stream().map(this::toCxcResponse).toList();
        }

        @Transactional
        public CuentaPorCobrarResponse registrarPago(Long idCuenta, PagoCxcRequest request) {
                CuentaPorCobrar cxc = cxcRepository.findById(idCuenta)
                                .orElseThrow(() -> new IllegalArgumentException("CXC no encontrada"));

                if ("pagada".equals(cxc.getEstatus())) {
                        throw new IllegalArgumentException("Esta cuenta ya está pagada");
                }

                BigDecimal nuevoMontoPagado = cxc.getMontoPagado().add(request.getMonto());

                if (nuevoMontoPagado.compareTo(cxc.getMontoTotal()) > 0) {
                        throw new IllegalArgumentException(
                                        "El pago excede el saldo pendiente de " +
                                                        cxc.getMontoTotal().subtract(cxc.getMontoPagado()));
                }

                // Registrar pago
                pagoCxcRepository.save(
                                PagoCxc.builder()
                                                .cuenta(cxc)
                                                .monto(request.getMonto())
                                                .metodoPago(request.getMetodoPago())
                                                .notas(request.getNotas())
                                                .build());

                // Actualizar monto pagado y estatus
                cxc.setMontoPagado(nuevoMontoPagado);
                cxc.setEstatus(
                                nuevoMontoPagado.compareTo(cxc.getMontoTotal()) >= 0
                                                ? "pagada"
                                                : "parcial");

                return toCxcResponse(cxcRepository.save(cxc));
        }

        // ─── Mappers ──────────────────────────────────────────────────────────────

        private VentaResponse toResponse(Venta v) {
                List<DetalleVentaResponse> detalles = v.getDetalles() == null
                                ? List.of()
                                : v.getDetalles().stream().map(this::toDetalleResponse).toList();

                boolean tieneCxc = cxcRepository.findByClienteIdOrderByFechaCreacionDesc(
                                v.getCliente() != null ? v.getCliente().getId() : -1).stream()
                                .anyMatch(c -> c.getVenta().getId().equals(v.getId()));

                return VentaResponse.builder()
                                .id(v.getId())
                                .idCliente(v.getCliente() != null ? v.getCliente().getId() : null)
                                .cliente(v.getCliente() != null ? v.getCliente().getNombre() : "Público general")
                                .usuario(v.getUsuario().getName())
                                .fecha(v.getFecha())
                                .total(v.getTotal())
                                .impuestos(v.getImpuestos())
                                .descuento(v.getDescuento())
                                .metodoPago(v.getMetodoPago())
                                .estatus(v.getEstatus())
                                .notas(v.getNotas())
                                .detalles(detalles)
                                .tieneCxc(tieneCxc)
                                .build();
        }

        private DetalleVentaResponse toDetalleResponse(DetalleVenta d) {
                ProductoVariante v = d.getVariante();
                return DetalleVentaResponse.builder()
                                .id(d.getId())
                                .idVariante(v.getId())
                                .sku(v.getSku())
                                .producto(v.getProducto().getNombre())
                                .atributos(v.getAtributosMap())
                                .imagenUrl(v.getImagenUrl() != null
                                                ? v.getImagenUrl()
                                                : v.getProducto().getImagenUrl())
                                .cantidad(d.getCantidad())
                                .precioUnitario(d.getPrecioUnitario())
                                .subtotal(d.getPrecioUnitario()
                                                .multiply(BigDecimal.valueOf(d.getCantidad())))
                                .build();
        }

        private CuentaPorCobrarResponse toCxcResponse(CuentaPorCobrar c) {
                return CuentaPorCobrarResponse.builder()
                                .id(c.getId())
                                .idVenta(c.getVenta().getId())
                                .idCliente(c.getCliente().getId())
                                .cliente(c.getCliente().getNombre())
                                .montoTotal(c.getMontoTotal())
                                .montoPagado(c.getMontoPagado())
                                .saldo(c.getMontoTotal().subtract(c.getMontoPagado()))
                                .estatus(c.getEstatus())
                                .fechaVencimiento(c.getFechaVencimiento())
                                .fechaCreacion(c.getFechaCreacion())
                                .vencida(c.getFechaVencimiento() != null
                                                && c.getFechaVencimiento().isBefore(LocalDate.now())
                                                && !"pagada".equals(c.getEstatus()))
                                .build();
        }

        @Transactional(readOnly = true)
        public List<ComisionResumenResponse> listarComisiones(String periodo) {
                List<ComisionVenta> lista = periodo != null
                                ? comisionRepository.findByPeriodo(periodo)
                                : comisionRepository.findAllByOrderByFechaCreacionDesc();

                return lista.stream().map(c -> ComisionResumenResponse.builder()
                                .id(c.getId())
                                .idVenta(c.getVenta().getId())
                                .usuario(c.getUsuario().getName())
                                .periodo(c.getPeriodo())
                                .montoComision(c.getMontoComision())
                                .estatus(c.getEstatus())
                                .fechaPago(c.getFechaPago())
                                .build()).toList();
        }

        @Transactional
        public void pagarComision(Long id) {
                ComisionVenta comision = comisionRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Comisión no encontrada"));

                if ("pagada".equals(comision.getEstatus())) {
                        throw new IllegalArgumentException("Esta comisión ya fue pagada");
                }

                comision.setEstatus("pagada");
                comision.setFechaPago(java.time.LocalDateTime.now());
                comisionRepository.save(comision);

                // Cargar categoría "Comisiones sobre Ventas (Personal)" — id 106
                CategoriaGasto categoria = categoriaGastoRepository.findById(106)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Categoría de comisiones no encontrada"));

                // Registrar como gasto automáticamente
                gastoRepository.save(Gasto.builder()
                                .categoria(categoria)
                                .usuario(comision.getUsuario())
                                .fecha(java.time.LocalDate.now())
                                .monto(comision.getMontoComision())
                                .descripcion(String.format(
                                                "Comisión venta #%04d — %s (período %s)",
                                                comision.getVenta().getId(),
                                                comision.getUsuario().getName(),
                                                comision.getPeriodo()))
                                .pagadoPor(comision.getUsuario())
                                .estatusReembolso(null)
                                .build());
        }

        @Transactional(readOnly = true)
        public List<ComisionResumenResponse> listarComisionesUsuario(
                        Integer idUsuario, String periodo) {

                List<ComisionVenta> lista = periodo != null
                                ? comisionRepository.findByUsuarioIdAndPeriodoOrderByFechaCreacionDesc(
                                                idUsuario, periodo)
                                : comisionRepository.findByUsuarioId(idUsuario);

                return lista.stream().map(c -> ComisionResumenResponse.builder()
                                .id(c.getId())
                                .idVenta(c.getVenta().getId())
                                .usuario(c.getUsuario().getName())
                                .periodo(c.getPeriodo())
                                .montoComision(c.getMontoComision())
                                .estatus(c.getEstatus())
                                .fechaPago(c.getFechaPago())
                                .build()).toList();
        }

        private BigDecimal calcularComision(
                        BigDecimal precioReal,
                        BigDecimal precioBase,
                        BigDecimal pctComision) {

                if (pctComision == null || pctComision.compareTo(BigDecimal.ZERO) == 0) {
                        return BigDecimal.ZERO;
                }

                // Comisión base sobre precio_venta de la variante
                BigDecimal comisionBase = precioBase
                                .multiply(pctComision)
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

                // Excedente = precio real - precio base
                BigDecimal excedente = precioReal.subtract(precioBase);

                // Si vendió por encima del precio base, 50% del excedente va al vendedor
                BigDecimal comisionExtra = excedente.compareTo(BigDecimal.ZERO) > 0
                                ? excedente.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                return comisionBase.add(comisionExtra).setScale(2, RoundingMode.HALF_UP);
        }
}