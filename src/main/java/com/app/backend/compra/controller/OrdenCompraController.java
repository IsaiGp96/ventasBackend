package com.app.backend.compra.controller;

import com.app.backend.compra.dto.CalculoPrecioRequest;
import com.app.backend.compra.dto.CalculoPrecioResponse;
import com.app.backend.compra.dto.OrdenCompraRequest;
import com.app.backend.compra.dto.OrdenCompraResponse;
import com.app.backend.compra.service.CalculoPrecioService;
import com.app.backend.compra.service.OrdenCompraPdfService;
import com.app.backend.compra.service.OrdenCompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;
    private final OrdenCompraPdfService pdfService;
    private final CalculoPrecioService calculoPrecioService;

    @GetMapping
    public ResponseEntity<List<OrdenCompraResponse>> listar() {
        return ResponseEntity.ok(ordenCompraService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompraResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(ordenCompraService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<OrdenCompraResponse> crear(
            @Valid @RequestBody OrdenCompraRequest request) {
        return ResponseEntity.ok(ordenCompraService.crear(request));
    }

    @PatchMapping("/{id}/recibir")
    public ResponseEntity<OrdenCompraResponse> recibir(@PathVariable Integer id) {
        return ResponseEntity.ok(ordenCompraService.marcarComoRecibida(id));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<OrdenCompraResponse> cancelar(@PathVariable Integer id) {
        return ResponseEntity.ok(ordenCompraService.cancelarOrden(id));
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<OrdenCompraResponse> confirmar(@PathVariable Integer id) {
        return ResponseEntity.ok(ordenCompraService.confirmarOrden(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        ordenCompraService.eliminarBorrador(id);
        return ResponseEntity.noContent().build();
    }

    // Editar un borrador
    @PutMapping("/{id}")
    public ResponseEntity<OrdenCompraResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody OrdenCompraRequest request) {
        return ResponseEntity.ok(ordenCompraService.actualizarBorrador(id, request));
    }

    // Generar PDF
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Integer id) {
        byte[] pdf = pdfService.generarPdf(id);
        String filename = String.format("OC-%04d.pdf", id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/calcular-precio")
    public ResponseEntity<CalculoPrecioResponse> calcularPrecio(
            @RequestBody CalculoPrecioRequest request) {
        return ResponseEntity.ok(calculoPrecioService.calcular(request));
    }

}
