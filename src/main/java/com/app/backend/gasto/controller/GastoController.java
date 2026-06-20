package com.app.backend.gasto.controller;

import com.app.backend.gasto.dto.*;
import com.app.backend.gasto.entity.CategoriaGasto;
import com.app.backend.gasto.service.GastoService;
import com.app.backend.user.entity.Role;
import com.app.backend.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/gastos")
@RequiredArgsConstructor
public class GastoController {

    private final GastoService gastoService;

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaResponse>> categorias() {
        return ResponseEntity.ok(gastoService.listarCategoriasNivel3());
    }

    @GetMapping
    public ResponseEntity<List<GastoResponse>> listar(
            @AuthenticationPrincipal User usuario) {
        boolean esAdmin = usuario.getRole() == Role.ADMIN;
        return ResponseEntity.ok(
                esAdmin
                        ? gastoService.listar()
                        : gastoService.listarPorUsuario(usuario.getId()));
    }

    @GetMapping("/periodo")
    public ResponseEntity<List<GastoResponse>> listarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(gastoService.listarPorPeriodo(inicio, fin));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenGastoResponse> resumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @AuthenticationPrincipal User usuario) {
        boolean esAdmin = usuario.getRole() == Role.ADMIN;

        return ResponseEntity.ok(
                esAdmin
                        ? gastoService.resumen(inicio, fin)
                        : gastoService.resumenPorUsuario(inicio, fin, usuario.getId()));
    }

    @PostMapping
    public ResponseEntity<GastoResponse> crear(
            @Valid @RequestBody GastoRequest request,
            @AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(gastoService.crear(request, usuario.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GastoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody GastoRequest request) {
        return ResponseEntity.ok(gastoService.actualizar(id, request));
    }

    @PostMapping(value = "/{id}/comprobante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GastoResponse> subirComprobante(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(gastoService.subirComprobante(id, file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        gastoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reembolsar")
    public ResponseEntity<GastoResponse> marcarReembolsado(@PathVariable Long id) {
        return ResponseEntity.ok(gastoService.marcarReembolsado(id));
    }

    @GetMapping("/reembolsos-pendientes")
    public ResponseEntity<List<GastoResponse>> reembolsosPendientes() {
        return ResponseEntity.ok(gastoService.listarReembolsosPendientes());
    }
}