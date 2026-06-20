package com.app.backend.producto.controller;

import com.app.backend.producto.dto.*;
import com.app.backend.producto.entity.*;
import com.app.backend.producto.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    // ─── Rutas fijas — DEBEN ir antes de /{id} ───────────────────────────────

    @GetMapping("/tipos")
    public ResponseEntity<List<TipoProducto>> listarTipos() {
        return ResponseEntity.ok(productoService.listarTipos());
    }

    @GetMapping("/catalogo")
    public ResponseEntity<List<java.util.Map<String, Object>>> catalogo(
            @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(productoService.obtenerCatalogoFlat(categoria));
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaTreeResponse>> categorias() {
        return ResponseEntity.ok(productoService.obtenerArbolCategorias());
    }

    // ─── Productos ────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar() {
        return ResponseEntity.ok(productoService.listarProductos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.obtenerProducto(id));
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.crearProducto(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizarProducto(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        productoService.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Variantes ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/variantes")
    public ResponseEntity<ProductoResponse.VarianteResponse> agregarVariante(
            @PathVariable Integer id,
            @Valid @RequestBody VarianteRequest request) {
        return ResponseEntity.ok(productoService.agregarVariante(id, request));
    }

    @DeleteMapping("/variantes/{id}")
    public ResponseEntity<Void> desactivarVariante(@PathVariable Integer id) {
        productoService.desactivarVariante(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/variantes/{id}/precios")
    public ResponseEntity<ProductoResponse.VarianteResponse> actualizarPrecios(
            @PathVariable Integer id,
            @RequestBody VariantePreciosRequest request) {
        return ResponseEntity.ok(productoService.actualizarPreciosVariante(id, request));
    }

    // ─── Imágenes ─────────────────────────────────────────────────────────────

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoResponse> subirImagenProducto(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productoService.subirImagenProducto(id, file));
    }

    @PostMapping(value = "/variantes/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoResponse.VarianteResponse> subirImagenVariante(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productoService.subirImagenVariante(id, file));
    }
}