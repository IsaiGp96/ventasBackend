package com.app.backend.inventario.controller;

import com.app.backend.compra.dto.OrdenCompraResponse;
import com.app.backend.inventario.dto.EntradaInventarioRequest;
import com.app.backend.inventario.dto.EntradaInventarioResponse;
import com.app.backend.inventario.dto.StockProductoResponse;
import com.app.backend.inventario.service.InventarioService;
import com.app.backend.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    // Órdenes confirmadas disponibles para recibir
    @GetMapping("/ordenes-pendientes")
    public ResponseEntity<List<OrdenCompraResponse>> ordenesPendientes() {
        return ResponseEntity.ok(inventarioService.listarOrdenesConfirmadas());
    }

    // Registrar entrada de mercancía
    @PostMapping("/entrada")
    public ResponseEntity<EntradaInventarioResponse> registrarEntrada(
            @Valid @RequestBody EntradaInventarioRequest request,
            @AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(
                inventarioService.registrarEntrada(request, usuario.getId()));
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockProductoResponse>> consultarStock() {
        return ResponseEntity.ok(inventarioService.consultarStock());
    }
}