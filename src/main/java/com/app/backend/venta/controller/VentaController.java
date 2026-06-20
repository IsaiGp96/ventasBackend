package com.app.backend.venta.controller;

import com.app.backend.user.entity.User;
import com.app.backend.user.repository.UsuarioPermisoRepository;
import com.app.backend.venta.dto.*;
import com.app.backend.venta.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;
    private final UsuarioPermisoRepository permisoRepository;

    @GetMapping
    public ResponseEntity<List<VentaResponse>> listar(@AuthenticationPrincipal User usuario) {

        boolean verTodas = usuario.getRole().name().equals("ADMIN") ||
                permisoRepository.findByUsuarioId(usuario.getId())
                        .stream()
                        .map(p -> p.getPermiso())
                        .anyMatch(p -> p.equals("ventas:ver_todas"));

        return ResponseEntity.ok(ventaService.listar(usuario.getId(), verTodas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(ventaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<VentaResponse> crear(
            @Valid @RequestBody VentaRequest request,
            @AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(ventaService.crear(request, usuario.getId()));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VentaResponse> cancelar(@PathVariable Integer id) {
        return ResponseEntity.ok(ventaService.cancelar(id));
    }

    @GetMapping("/cxc")
    public ResponseEntity<List<CuentaPorCobrarResponse>> cxcPendientes() {
        return ResponseEntity.ok(ventaService.listarCxcPendientes());
    }

    @PostMapping("/cxc/{id}/pago")
    public ResponseEntity<CuentaPorCobrarResponse> registrarPago(
            @PathVariable Long id,
            @Valid @RequestBody PagoCxcRequest request) {
        return ResponseEntity.ok(ventaService.registrarPago(id, request));
    }

    @GetMapping("/comisiones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComisionResumenResponse>> comisiones(
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(ventaService.listarComisiones(periodo));
    }

    @GetMapping("/comisiones/mias")
    public ResponseEntity<List<ComisionResumenResponse>> misComisiones(
            @AuthenticationPrincipal User usuario,
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(
                ventaService.listarComisionesUsuario(usuario.getId(), periodo));
    }

    @PatchMapping("/comisiones/{id}/pagar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> pagarComision(@PathVariable Long id) {
        ventaService.pagarComision(id);
        return ResponseEntity.noContent().build();
    }
}