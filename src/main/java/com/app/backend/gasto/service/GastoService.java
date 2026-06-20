package com.app.backend.gasto.service;

import com.app.backend.gasto.dto.*;
import com.app.backend.gasto.entity.*;
import com.app.backend.gasto.repository.*;
import com.app.backend.user.entity.User;
import com.app.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.app.backend.producto.service.FileStorageService;
import com.app.backend.proveedor.entity.Proveedor;
import com.app.backend.proveedor.repository.ProveedorRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GastoService {

        private final GastoRepository gastoRepository;
        private final CategoriaGastoRepository categoriaRepository;
        private final UserRepository userRepository;
        private final FileStorageService fileStorageService;
        private final ProveedorRepository proveedorRepository;

        // ─── Catálogo ─────────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<CategoriaResponse> listarCategoriasNivel3() {
                return categoriaRepository
                                .findNivel3ConArbol(3)
                                .stream()
                                .map(this::toCategoriaResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<CategoriaGasto> listarArbol() {
                return categoriaRepository.findByActivoTrueOrderByNivelAscNombreAsc();
        }

        // ─── CRUD Gastos ──────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<GastoResponse> listar() {
                return gastoRepository.findAllByOrderByFechaDescFechaCreacionDesc()
                                .stream().map(this::toResponse).toList();
        }

        @Transactional(readOnly = true)
        public List<GastoResponse> listarPorPeriodo(LocalDate inicio, LocalDate fin) {
                return gastoRepository.findByPeriodo(inicio, fin)
                                .stream().map(this::toResponse).toList();
        }

        @Transactional(readOnly = true)
        public List<GastoResponse> listarPorUsuario(Integer idUsuario) {
                return gastoRepository.findByUsuarioIdOrderByFechaDesc(idUsuario)
                                .stream().map(this::toResponse).toList();
        }

        @Transactional
        public GastoResponse crear(GastoRequest request, Integer idUsuario) {
                CategoriaGasto categoria = categoriaRepository.findById(request.getIdCategoria())
                                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));

                if (categoria.getNivel() != 3) {
                        throw new IllegalArgumentException(
                                        "Selecciona una categoría específica (nivel 3)");
                }

                Proveedor proveedor = null;
                if (request.getIdproveedor() != null) {
                        proveedor = proveedorRepository.findById(request.getIdproveedor())
                                        .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
                }

                User pagadoPor = null;
                if (request.getIdPagadoPor() != null) {
                        pagadoPor = userRepository.findById(request.getIdPagadoPor())
                                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                }

                User usuario = userRepository.findById(idUsuario)
                                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

                Gasto gasto = Gasto.builder()
                                .categoria(categoria)
                                .usuario(usuario)
                                .fecha(request.getFecha())
                                .monto(request.getMonto())
                                .descripcion(request.getDescripcion())
                                .proveedor(proveedor)
                                .pagadoPor(pagadoPor)
                                .estatusReembolso(pagadoPor != null ? "pendiente" : null)
                                .build();

                return toResponse(gastoRepository.save(gasto));
        }

        @Transactional
        public GastoResponse subirComprobante(Long idGasto, MultipartFile file) {
                Gasto gasto = gastoRepository.findById(idGasto)
                                .orElseThrow(() -> new IllegalArgumentException("Gasto no encontrado"));

                fileStorageService.eliminarImagen(gasto.getComprobanteUrl());
                String url = fileStorageService.guardarImagen(file, "comprobantes");
                gasto.setComprobanteUrl(url);
                return toResponse(gastoRepository.save(gasto));
        }

        @Transactional
        public GastoResponse actualizar(Long id, GastoRequest request) {
                Gasto gasto = gastoRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Gasto no encontrado"));

                CategoriaGasto categoria = categoriaRepository.findById(request.getIdCategoria())
                                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
                Proveedor proveedor = null;
                if (request.getIdproveedor() != null) {
                        proveedor = proveedorRepository.findById(request.getIdproveedor())
                                        .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
                }

                User pagadoPor = null;

                if (request.getIdPagadoPor() != null) {
                        pagadoPor = userRepository.findById(request.getIdPagadoPor())
                                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                }
                gasto.setPagadoPor(pagadoPor);

                if (pagadoPor != null && gasto.getEstatusReembolso() == null) {
                        gasto.setEstatusReembolso("pendiente");
                }
                if (pagadoPor == null) {
                        gasto.setEstatusReembolso(null);
                        gasto.setFechaReembolso(null);
                }
                gasto.setCategoria(categoria);
                gasto.setFecha(request.getFecha());
                gasto.setMonto(request.getMonto());
                gasto.setDescripcion(request.getDescripcion());
                gasto.setProveedor(proveedor);
                return toResponse(gastoRepository.save(gasto));
        }

        @Transactional
        public void eliminar(Long id) {
                Gasto gasto = gastoRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Gasto no encontrado"));
                fileStorageService.eliminarImagen(gasto.getComprobanteUrl());
                gastoRepository.delete(gasto);
        }

        // ─── Resumen ──────────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public ResumenGastoResponse resumen(LocalDate inicio, LocalDate fin) {
                List<Gasto> gastos = gastoRepository.findByPeriodo(inicio, fin);

                BigDecimal total = gastos.stream()
                                .map(Gasto::getMonto)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal fijos = gastos.stream()
                                .filter(g -> "fijo".equals(g.getCategoria().getTipo()))
                                .map(Gasto::getMonto)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal variables = total.subtract(fijos);

                // Agrupado por categoría nivel 3
                List<ResumenGastoResponse.ResumenCategoria> porCategoria = gastos.stream()
                                .collect(Collectors.groupingBy(
                                                g -> g.getCategoria().getId(),
                                                Collectors.toList()))
                                .values().stream()
                                .map(lista -> {
                                        Gasto primero = lista.get(0);
                                        CategoriaGasto cat = primero.getCategoria();
                                        CategoriaGasto subcat = cat.getPadre();
                                        String grupo = subcat != null && subcat.getPadre() != null
                                                        ? subcat.getPadre().getNombre()
                                                        : "";

                                        return ResumenGastoResponse.ResumenCategoria.builder()
                                                        .grupo(grupo)
                                                        .subcategoria(subcat != null ? subcat.getNombre() : "")
                                                        .categoria(cat.getNombre())
                                                        .tipo(cat.getTipo())
                                                        .total(lista.stream().map(Gasto::getMonto)
                                                                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                                                        .cantidad((long) lista.size())
                                                        .build();
                                })
                                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                                .toList();

                // Agrupado por mes
                DateTimeFormatter fmtMes = DateTimeFormatter.ofPattern("yyyy-MM");
                List<ResumenGastoResponse.ResumenMes> porMes = gastos.stream()
                                .collect(Collectors.groupingBy(
                                                g -> g.getFecha().format(fmtMes),
                                                Collectors.toList()))
                                .entrySet().stream()
                                .map(e -> ResumenGastoResponse.ResumenMes.builder()
                                                .periodo(e.getKey())
                                                .total(e.getValue().stream().map(Gasto::getMonto)
                                                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                                                .cantidad((long) e.getValue().size())
                                                .build())
                                .sorted((a, b) -> b.getPeriodo().compareTo(a.getPeriodo()))
                                .toList();
                List<GastoResponse> reembolsosPendientes = gastoRepository
                                .findByEstatusReembolso("pendiente")
                                .stream().map(this::toResponse).toList();
                return ResumenGastoResponse.builder()
                                .totalPeriodo(total)
                                .totalFijos(fijos)
                                .totalVariables(variables)
                                .porCategoria(porCategoria)
                                .porMes(porMes)
                                .reembolsosPendientes(reembolsosPendientes)
                                .build();
        }

        @Transactional(readOnly = true)
        public ResumenGastoResponse resumenPorUsuario(LocalDate inicio, LocalDate fin, Integer usuarioInteger) {
                List<Gasto> gastos = gastoRepository.findByPeriodoUsuario(inicio, fin, usuarioInteger);

                BigDecimal total = gastos.stream()
                                .map(Gasto::getMonto)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal fijos = gastos.stream()
                                .filter(g -> "fijo".equals(g.getCategoria().getTipo()))
                                .map(Gasto::getMonto)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal variables = total.subtract(fijos);

                // Agrupado por categoría nivel 3
                List<ResumenGastoResponse.ResumenCategoria> porCategoria = gastos.stream()
                                .collect(Collectors.groupingBy(
                                                g -> g.getCategoria().getId(),
                                                Collectors.toList()))
                                .values().stream()
                                .map(lista -> {
                                        Gasto primero = lista.get(0);
                                        CategoriaGasto cat = primero.getCategoria();
                                        CategoriaGasto subcat = cat.getPadre();
                                        String grupo = subcat != null && subcat.getPadre() != null
                                                        ? subcat.getPadre().getNombre()
                                                        : "";

                                        return ResumenGastoResponse.ResumenCategoria.builder()
                                                        .grupo(grupo)
                                                        .subcategoria(subcat != null ? subcat.getNombre() : "")
                                                        .categoria(cat.getNombre())
                                                        .tipo(cat.getTipo())
                                                        .total(lista.stream().map(Gasto::getMonto)
                                                                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                                                        .cantidad((long) lista.size())
                                                        .build();
                                })
                                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                                .toList();

                // Agrupado por mes
                DateTimeFormatter fmtMes = DateTimeFormatter.ofPattern("yyyy-MM");
                List<ResumenGastoResponse.ResumenMes> porMes = gastos.stream()
                                .collect(Collectors.groupingBy(
                                                g -> g.getFecha().format(fmtMes),
                                                Collectors.toList()))
                                .entrySet().stream()
                                .map(e -> ResumenGastoResponse.ResumenMes.builder()
                                                .periodo(e.getKey())
                                                .total(e.getValue().stream().map(Gasto::getMonto)
                                                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                                                .cantidad((long) e.getValue().size())
                                                .build())
                                .sorted((a, b) -> b.getPeriodo().compareTo(a.getPeriodo()))
                                .toList();
                List<GastoResponse> reembolsosPendientes = gastoRepository
                                .findByEstatusReembolso("pendiente")
                                .stream()
                                .filter(g -> g.getUsuario().getId().equals(usuarioInteger))
                                .map(this::toResponse).toList();
                return ResumenGastoResponse.builder()
                                .totalPeriodo(total)
                                .totalFijos(fijos)
                                .totalVariables(variables)
                                .porCategoria(porCategoria)
                                .porMes(porMes)
                                .reembolsosPendientes(reembolsosPendientes)
                                .build();
        }

        // ─── Mapper ───────────────────────────────────────────────────────────────

        private GastoResponse toResponse(Gasto g) {
                CategoriaGasto cat = g.getCategoria();
                CategoriaGasto subcat = cat.getPadre();
                String grupo = subcat != null && subcat.getPadre() != null
                                ? subcat.getPadre().getNombre()
                                : "";

                return GastoResponse.builder()
                                .id(g.getId())
                                .fecha(g.getFecha())
                                .monto(g.getMonto())
                                .descripcion(g.getDescripcion())
                                .idProveedor(g.getProveedor() != null ? g.getProveedor().getId() : null)
                                .proveedor(g.getProveedor() != null ? g.getProveedor().getNombre() : null)
                                .comprobanteUrl(g.getComprobanteUrl())
                                .categoria(cat.getNombre())
                                .subcategoria(subcat != null ? subcat.getNombre() : "")
                                .grupo(grupo)
                                .tipo(cat.getTipo())
                                .idCategoria(cat.getId())
                                .idPagadoPor(g.getPagadoPor() != null ? g.getPagadoPor().getId() : null)
                                .pagadoPor(g.getPagadoPor() != null ? g.getPagadoPor().getName() : null)
                                .estatusReembolso(g.getEstatusReembolso())
                                .fechaReembolso(g.getFechaReembolso())
                                .esReembolso(g.getPagadoPor() != null)
                                .build();
        }

        private CategoriaResponse toCategoriaResponse(CategoriaGasto cat) {
                // cat es nivel 3, cat.getPadre() es nivel 2, cat.getPadre().getPadre() es nivel
                // 1
                CategoriaGasto subcat = cat.getPadre();
                String subcatNombre = subcat != null ? subcat.getNombre() : "";
                String grupoNombre = subcat != null && subcat.getPadre() != null
                                ? subcat.getPadre().getNombre()
                                : "";

                return CategoriaResponse.builder()
                                .id(cat.getId())
                                .nombre(cat.getNombre())
                                .tipo(cat.getTipo())
                                .subcategoria(subcatNombre)
                                .grupo(grupoNombre)
                                .build();
        }

        @Transactional
        public GastoResponse marcarReembolsado(Long id) {
                Gasto gasto = gastoRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Gasto no encontrado"));

                if (!"pendiente".equals(gasto.getEstatusReembolso())) {
                        throw new IllegalArgumentException(
                                        "Este gasto no tiene un reembolso pendiente");
                }

                gasto.setEstatusReembolso("pagado");
                gasto.setFechaReembolso(java.time.LocalDate.now());
                return toResponse(gastoRepository.save(gasto));
        }

        // Listar reembolsos pendientes — para el resumen de saldos
        @Transactional(readOnly = true)
        public List<GastoResponse> listarReembolsosPendientes() {
                return gastoRepository.findByEstatusReembolso("pendiente")
                                .stream().map(this::toResponse).toList();
        }
}
