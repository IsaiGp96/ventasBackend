package com.app.backend.producto.service;

import com.app.backend.producto.dto.*;
import com.app.backend.producto.entity.*;
import com.app.backend.producto.repository.*;
import com.app.backend.proveedor.entity.Proveedor;
import com.app.backend.proveedor.repository.ProveedorRepository;

import jakarta.persistence.EntityManager;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

        private final EntityManager entityManager;
        private final ProductoRepository productoRepository;
        private final ProductoVarianteRepository varianteRepository;
        private final TipoProductoRepository tipoProductoRepository;
        private final ProveedorRepository proveedorRepository;
        private final FileStorageService fileStorageService;
        private final VarianteAtributoRepository varianteAtributoRepository;
        private final CategoriaProductoRepository categoriaProductoRepository;

        // ─── Catálogos ────────────────────────────────────────────────────────────

        public List<TipoProducto> listarTipos() {
                return tipoProductoRepository.findByActivoTrue();
        }

        // ─── Productos ────────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<ProductoResponse> listarProductos() {
                return productoRepository.findByActivoTrue()
                                .stream()
                                .map(this::toProductoResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public ProductoResponse obtenerProducto(Integer id) {
                Producto producto = productoRepository.findByIdWithVariantes(id)
                                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
                return toProductoResponse(producto);
        }

        @Transactional
        public ProductoResponse crearProducto(ProductoRequest request) {
                TipoProducto tipo = tipoProductoRepository.findById(request.getIdTipoProducto())
                                .orElseThrow(() -> new IllegalArgumentException("Tipo de producto no encontrado"));

                Proveedor proveedor = null;
                if (request.getIdProveedor() != null) {
                        proveedor = proveedorRepository.findById(request.getIdProveedor())
                                        .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
                }

                CategoriaProducto categoria = null;
                if (request.getIdCategoria() != null) {
                        categoria = categoriaProductoRepository.findById(request.getIdCategoria())
                                        .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
                }
                Producto producto = Producto.builder()
                                .nombre(request.getNombre())
                                .descripcion(request.getDescripcion())
                                .tipoProducto(tipo)
                                .proveedor(proveedor)
                                .categoria(categoria)
                                .precioCompra(request.getPrecioCompra())
                                .precioVenta(request.getPrecioVenta())
                                .activo(true)
                                .build();

                return toProductoResponse(productoRepository.save(producto));
        }

        @Transactional
        public ProductoResponse actualizarProducto(Integer id, ProductoRequest request) {
                Producto producto = productoRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

                TipoProducto tipo = tipoProductoRepository.findById(request.getIdTipoProducto())
                                .orElseThrow(() -> new IllegalArgumentException("Tipo de producto no encontrado"));

                Proveedor proveedor = null;
                if (request.getIdProveedor() != null) {
                        proveedor = proveedorRepository.findById(request.getIdProveedor())
                                        .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
                }

                if (request.getIdCategoria() != null) {
                        CategoriaProducto categoria = categoriaProductoRepository
                                        .findById(request.getIdCategoria())
                                        .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
                        producto.setCategoria(categoria);
                } else {
                        producto.setCategoria(null);
                }

                producto.setNombre(request.getNombre());
                producto.setDescripcion(request.getDescripcion());
                producto.setTipoProducto(tipo);
                producto.setProveedor(proveedor);
                producto.setPrecioCompra(request.getPrecioCompra());
                producto.setPrecioVenta(request.getPrecioVenta());

                return toProductoResponse(productoRepository.save(producto));
        }

        @Transactional
        public void desactivarProducto(Integer id) {
                Producto producto = productoRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
                producto.setActivo(false);
                productoRepository.save(producto);
        }

        // ─── Variantes ────────────────────────────────────────────────────────────

        @Transactional
        public void desactivarVariante(Integer idVariante) {
                ProductoVariante variante = varianteRepository.findById(idVariante)
                                .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));
                variante.setActivo(false);
                varianteRepository.save(variante);
        }

        // ─── Mappers ──────────────────────────────────────────────────────────────

        private ProductoResponse toProductoResponse(Producto p) {
                List<ProductoResponse.VarianteResponse> variantes = p.getVariantes() == null
                                ? List.of()
                                : p.getVariantes().stream()
                                                .filter(v -> Boolean.TRUE.equals(v.getActivo()))
                                                .map(this::toVarianteResponse)
                                                .toList();

                return ProductoResponse.builder()
                                .id(p.getId())
                                .nombre(p.getNombre())
                                .descripcion(p.getDescripcion())
                                .idTipoProducto(p.getTipoProducto().getId())
                                .tipoProducto(p.getTipoProducto().getNombre())
                                .idProveedor(p.getProveedor() != null ? p.getProveedor().getId() : null)
                                .proveedor(p.getProveedor() != null ? p.getProveedor().getNombre() : null)
                                .idCategoria(p.getCategoria() != null ? p.getCategoria().getId() : null)
                                .categoria(p.getCategoria() != null ? p.getCategoria().getNombre() : null)
                                .categoriaSlug(p.getCategoria() != null ? p.getCategoria().getSlug() : null)
                                .precioCompra(p.getPrecioCompra())
                                .precioVenta(p.getPrecioVenta())
                                .activo(p.getActivo())
                                .imagenUrl(p.getImagenUrl())
                                .variantes(variantes)
                                .build();
        }

        private ProductoResponse.VarianteResponse toVarianteResponse(ProductoVariante v) {
                Map<String, String> atributos = v.getAtributos() == null ? Map.of()
                                : v.getAtributos().stream()
                                                .collect(java.util.stream.Collectors.toMap(
                                                                VarianteAtributo::getNombre,
                                                                VarianteAtributo::getValor));

                return ProductoResponse.VarianteResponse.builder()
                                .id(v.getId())
                                .sku(v.getSku())
                                .codigoBarras(v.getCodigoBarras())
                                .atributos(atributos)
                                .precioCompra(v.getPrecioCompra())
                                .precioVenta(v.getPrecioVenta())
                                .pctMargen(v.getPctMargen())
                                .pctComision(v.getPctComision())
                                .stock(v.getStock() != null ? v.getStock().getCantidad() : 0)
                                .activo(v.getActivo())
                                .imagenUrl(v.getImagenUrl())
                                .build();
        }

        // ─── Imágenes
        // ─────────────────────────────────────────────────────────────────

        @Transactional
        public ProductoResponse subirImagenProducto(Integer id, MultipartFile file) {
                Producto producto = productoRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

                // Elimina imagen anterior si existe
                fileStorageService.eliminarImagen(producto.getImagenUrl());

                String url = fileStorageService.guardarImagen(file, "productos");
                producto.setImagenUrl(url);
                return toProductoResponse(productoRepository.save(producto));
        }

        @Transactional
        public ProductoResponse.VarianteResponse subirImagenVariante(Integer idVariante, MultipartFile file) {
                ProductoVariante variante = varianteRepository.findById(idVariante)
                                .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));

                fileStorageService.eliminarImagen(variante.getImagenUrl());

                String url = fileStorageService.guardarImagen(file, "productos/variantes");
                variante.setImagenUrl(url);
                return toVarianteResponse(varianteRepository.save(variante));
        }

        @Transactional(readOnly = true)
        @SuppressWarnings("unchecked")
        public List<java.util.Map<String, Object>> obtenerCatalogoFlat(String slug) {

                String filtro = (slug != null && !slug.isBlank())
                                ? """
                                                AND (
                                                    cp.slug = '%s'
                                                    OR cp_padre.slug = '%s'
                                                    OR cp_abuelo.slug = '%s'
                                                )
                                                """.formatted(slug, slug, slug)
                                : "";

                List<Object[]> rows = entityManager.createNativeQuery("""
                                SELECT
                                    pv.id                                        AS idVariante,
                                    pv.sku                                       AS sku,
                                    p.id                                         AS idProducto,
                                    p.nombre                                     AS nombre,
                                    p.descripcion                                AS descripcion,
                                    tp.nombre                                    AS tipo,
                                    COALESCE(pv.precio_venta, p.precio_venta, 0) AS precioVenta,
                                    COALESCE(sv.cantidad, 0)                     AS stock,
                                    COALESCE(pv.imagen_url, p.imagen_url)        AS imagenUrl,
                                    COALESCE(
                                        (SELECT json_object_agg(va.nombre, va.valor)
                                         FROM variante_atributo va
                                         WHERE va.id_variante = pv.id),
                                        '{}'::json
                                    )::text                                      AS atributos,
                                    cp.nombre                                    AS categoria,
                                    cp.slug                                      AS categoriaSlug
                                FROM producto_variante pv
                                JOIN producto p              ON p.id   = pv.id_producto
                                JOIN tipo_producto tp        ON tp.id  = p.id_tipo_producto
                                LEFT JOIN stock_variante sv  ON sv.id_variante = pv.id
                                LEFT JOIN categoria_producto cp        ON cp.id        = p.id_categoria
                                LEFT JOIN categoria_producto cp_padre  ON cp_padre.id  = cp.id_padre
                                LEFT JOIN categoria_producto cp_abuelo ON cp_abuelo.id = cp_padre.id_padre
                                WHERE p.activo  = true
                                  AND pv.activo = true
                                  AND COALESCE(sv.cantidad, 0) > 0
                                """ + filtro + """
                                ORDER BY p.nombre, pv.sku
                                """).getResultList();

                return rows.stream().map(r -> {
                        var m = new java.util.LinkedHashMap<String, Object>();
                        m.put("idVariante", ((Number) r[0]).intValue());
                        m.put("sku", r[1]);
                        m.put("idProducto", ((Number) r[2]).intValue());
                        m.put("nombre", r[3]);
                        m.put("descripcion", r[4]);
                        m.put("tipo", r[5]);
                        m.put("precioVenta", ((Number) r[6]).doubleValue());
                        m.put("stock", ((Number) r[7]).intValue());
                        m.put("imagenUrl", r[8]);
                        m.put("categoria", r[10]);
                        m.put("categoriaSlug", r[11]);
                        try {
                                String json = r[9] != null ? r[9].toString() : "{}";
                                m.put("atributos", new com.fasterxml.jackson.databind.ObjectMapper()
                                                .readValue(json, java.util.Map.class));
                        } catch (Exception e) {
                                m.put("atributos", java.util.Map.of());
                        }
                        return (java.util.Map<String, Object>) m;
                }).toList();
        }

        @Transactional
        public ProductoResponse.VarianteResponse agregarVariante(
                        Integer idProducto, VarianteRequest request) {

                Producto producto = productoRepository.findById(idProducto)
                                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

                if (varianteRepository.existsBySku(request.getSku())) {
                        throw new IllegalArgumentException("El SKU ya existe: " + request.getSku());
                }

                if (request.getAtributos() == null || request.getAtributos().isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Debe especificar al menos un atributo para la variante");
                }

                ProductoVariante variante = ProductoVariante.builder()
                                .producto(producto)
                                .sku(request.getSku())
                                .codigoBarras(request.getCodigoBarras())
                                .activo(true)
                                .build();

                variante = varianteRepository.save(variante);

                // Guardar atributos flexibles
                for (Map.Entry<String, String> entry : request.getAtributos().entrySet()) {
                        varianteAtributoRepository.save(
                                        VarianteAtributo.builder()
                                                        .variante(variante)
                                                        .nombre(entry.getKey().toLowerCase().trim())
                                                        .valor(entry.getValue().trim())
                                                        .build());
                }

                return toVarianteResponse(variante);
        }

        @Transactional
        public ProductoResponse.VarianteResponse actualizarPreciosVariante(
                        Integer id, VariantePreciosRequest request) {

                ProductoVariante variante = varianteRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Variante no encontrada"));

                variante.setPctMargen(request.getPctMargen());
                variante.setPctComision(request.getPctComision());

                // Recalcular precio_venta si hay costo disponible
                if (variante.getPrecioCompra() != null) {
                        BigDecimal precioVenta = calcularPrecioVenta(
                                        variante.getPrecioCompra(),
                                        request.getPctMargen(),
                                        request.getPctComision());
                        variante.setPrecioVenta(precioVenta);
                }

                varianteRepository.save(variante);
                return toVarianteResponse(variante);
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

        @Transactional(readOnly = true)
        public List<CategoriaTreeResponse> obtenerArbolCategorias() {
                // Cargar TODAS las categorías activas en una sola query
                List<CategoriaProducto> todas = categoriaProductoRepository
                                .findAllByActivoTrueOrderByNivelAscOrdenAsc();

                // Construir el árbol en memoria
                Map<Integer, CategoriaTreeResponse> mapa = new java.util.LinkedHashMap<>();
                List<CategoriaTreeResponse> raices = new java.util.ArrayList<>();

                // Primera pasada — crear todos los nodos
                for (CategoriaProducto c : todas) {
                        mapa.put(c.getId(), CategoriaTreeResponse.builder()
                                        .id(c.getId())
                                        .nombre(c.getNombre())
                                        .slug(c.getSlug())
                                        .nivel(c.getNivel())
                                        .hijos(new java.util.ArrayList<>())
                                        .build());
                }

                // Segunda pasada — conectar padres e hijos
                for (CategoriaProducto c : todas) {
                        CategoriaTreeResponse nodo = mapa.get(c.getId());
                        if (c.getPadre() == null) {
                                raices.add(nodo);
                        } else {
                                CategoriaTreeResponse padre = mapa.get(c.getPadre().getId());
                                if (padre != null)
                                        padre.getHijos().add(nodo);
                        }
                }

                return raices;
        }

        private CategoriaTreeResponse toCategoriaTree(CategoriaProducto c) {
                List<CategoriaTreeResponse> hijos = c.getHijos() == null ? List.of()
                                : c.getHijos().stream()
                                                .filter(h -> Boolean.TRUE.equals(h.getActivo()))
                                                .map(this::toCategoriaTree)
                                                .toList();

                return CategoriaTreeResponse.builder()
                                .id(c.getId())
                                .nombre(c.getNombre())
                                .slug(c.getSlug())
                                .nivel(c.getNivel())
                                .hijos(hijos)
                                .build();
        }
}