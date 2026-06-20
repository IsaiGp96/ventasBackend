package com.app.backend.dashboard.controller;

import com.app.backend.dashboard.dto.DashboardResponse;
import com.app.backend.dashboard.service.DashboardService;
import com.app.backend.user.repository.UsuarioPermisoRepository;
import com.app.backend.user.entity.User;

import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;
    private final UsuarioPermisoRepository permisoRepository;

    @GetMapping
    public ResponseEntity<DashboardResponse> obtener(
            @AuthenticationPrincipal User usuario) {

        boolean verTodo = usuario.getRole().name().equals("ADMIN") ||
                permisoRepository.findByUsuarioId(usuario.getId())
                        .stream()
                        .map(p -> p.getPermiso())
                        .collect(Collectors.toSet())
                        .contains("dashboard:ver_todo");

        return ResponseEntity.ok(dashboardService.obtener(usuario.getId(), verTodo));
    }
}