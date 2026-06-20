package com.app.backend.user.controller;

import com.app.backend.user.entity.Role;
import com.app.backend.user.entity.User;
import com.app.backend.user.entity.UsuarioPermiso;
import com.app.backend.user.repository.UserRepository;
import com.app.backend.user.repository.UsuarioPermisoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioPermisoRepository permisoRepository;

    // ─── DTOs inline ──────────────────────────────────────────────────────────

    @Data
    static class UsuarioResponse {
        Integer id;
        String nombre;
        String email;
        String rol;
        Boolean activo;
        LocalDateTime fechaCreacion;
        Set<String> permisos;
    }

    @Data
    static class CambiarPasswordRequest {
        @NotBlank
        String passwordActual;
        @NotBlank
        @Size(min = 8)
        String passwordNueva;
    }

    @Data
    static class CrearUsuarioRequest {
        @NotBlank
        String nombre;
        @NotBlank
        @Email
        String email;
        @NotBlank
        @Size(min = 8)
        String password;
        @NotBlank
        String rol; // ADMIN | EMPLOYEE | SUPERVISOR
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private UsuarioResponse toResponse(User u) {
        var r = new UsuarioResponse();
        r.id = u.getId();
        r.nombre = u.getName();
        r.email = u.getEmail();
        r.rol = u.getRole() != null ? u.getRole().name() : null;
        r.activo = u.getActivo();
        r.fechaCreacion = u.getCreatedAt();
        r.permisos = permisoRepository.findByUsuarioId(u.getId())
                .stream()
                .map(UsuarioPermiso::getPermiso)
                .collect(Collectors.toSet());
        return r;
    }

    // ─── Perfil ───────────────────────────────────────────────────────────────

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponse> perfil(
            @AuthenticationPrincipal User usuario) {
        return ResponseEntity.ok(toResponse(usuario));
    }

    @PatchMapping("/perfil/password")
    public ResponseEntity<Void> cambiarPassword(
            @AuthenticationPrincipal User usuario,
            @Valid @RequestBody CambiarPasswordRequest request) {

        if (!passwordEncoder.matches(
                request.getPasswordActual(), usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        User u = userRepository.findById(usuario.getId()).orElseThrow();
        u.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        userRepository.save(u);
        return ResponseEntity.noContent().build();
    }

    // ─── Listado ──────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(
                userRepository.findAllByOrderByCreatedAtDesc()
                        .stream().map(this::toResponse).toList());
    }

    @GetMapping("/activos")
    public ResponseEntity<List<Map<String, Object>>> listarActivos() {
        return ResponseEntity.ok(
                userRepository.findAll().stream()
                        .filter(User::getActivo)
                        .map(u -> Map.of(
                                "id", (Object) u.getId(),
                                "nombre", u.getName(),
                                "email", u.getEmail()))
                        .toList());
    }

    // ─── Crear ────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<UsuarioResponse> crear(
            @Valid @RequestBody CrearUsuarioRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        User nuevo = User.builder()
                .name(request.getNombre())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.valueOf(request.getRol()))
                .build();

        nuevo = userRepository.save(nuevo);

        // Asignar permisos default según rol
        Set<String> permisosDefault = permisosDefaultPorRol(nuevo.getRole());
        final User usuarioFinal = nuevo;
        permisosDefault.forEach(p -> permisoRepository.save(
                UsuarioPermiso.builder()
                        .usuario(usuarioFinal)
                        .permiso(p)
                        .build()));

        return ResponseEntity.ok(toResponse(userRepository.findById(nuevo.getId()).orElseThrow()));
    }

    // ─── Estatus ──────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/estatus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> toggleEstatus(
            @PathVariable Integer id,
            @AuthenticationPrincipal User admin) {

        if (id.equals(admin.getId())) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta");
        }

        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        u.setActive(!u.getActivo());
        return ResponseEntity.ok(toResponse(userRepository.save(u)));
    }

    // ─── Permisos ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}/permisos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<String>> obtenerPermisos(@PathVariable Integer id) {
        return ResponseEntity.ok(
                permisoRepository.findByUsuarioId(id)
                        .stream().map(UsuarioPermiso::getPermiso)
                        .collect(Collectors.toSet()));
    }

    @PutMapping("/{id}/permisos")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> actualizarPermisos(
            @PathVariable Integer id,
            @RequestBody Set<String> permisos) {

        User usuario = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        permisoRepository.deleteByUsuarioId(id);
        permisoRepository.flush();

        permisos.forEach(p -> permisoRepository.save(
                UsuarioPermiso.builder()
                        .usuario(usuario)
                        .permiso(p)
                        .build()));

        return ResponseEntity.noContent().build();
    }

    // ─── Helper permisos default ──────────────────────────────────────────────

    private Set<String> permisosDefaultPorRol(Role rol) {
        return switch (rol) {
            case ADMIN -> Set.of(
                    "dashboard:ver_todo",
                    "ventas:ver_todas", "ventas:crear",
                    "compras:ver_todas", "compras:crear",
                    "gastos:ver_todos",
                    "precios:ajustar",
                    "clientes:gestionar",
                    "inventario:gestionar",
                    "usuarios:gestionar");
            case EMPLOYEE -> Set.of(
                    "ventas:crear",
                    "clientes:gestionar");
            default -> Set.of(
                    "ventas:crear",
                    "clientes:gestionar");
        };
    }
}