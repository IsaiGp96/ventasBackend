package com.app.backend.auth.service;

import com.app.backend.auth.dto.AuthResponse;
import com.app.backend.auth.dto.LoginRequest;
import com.app.backend.auth.dto.RegisterRequest;
import com.app.backend.security.JwtService;
import com.app.backend.security.service.TokenBlacklistService;
import com.app.backend.user.entity.Role;
import com.app.backend.user.entity.User;
import com.app.backend.user.entity.UsuarioPermiso;
import com.app.backend.user.repository.UserRepository;
import com.app.backend.user.repository.UsuarioPermisoRepository;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UsuarioPermisoRepository permisoRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        User user = User.builder()
                .name(request.getNombre()) // ← getNombre()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPwd())) // ← getContrasena()
                .role(Role.EMPLOYEE)
                .active(true)
                .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPwd()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        Set<String> permisos = permisoRepository.findByUsuarioId(user.getId())
                .stream()
                .map(UsuarioPermiso::getPermiso)
                .collect(java.util.stream.Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .permisos(permisos)
                .build();
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        // Extrae el email del refresh token
        String email;
        try {
            email = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new IllegalArgumentException("Refresh token inválido");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Valida que el refresh token sea válido
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Refresh token expirado");
        }

        // Genera nuevos tokens
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String token) {
        try {
            String email = jwtService.extractUsername(token);
            // Extraer fecha de expiración del token para saber cuándo limpiar
            java.util.Date expiracion = jwtService.extractClaim(token,
                    claims -> claims.getExpiration());
            LocalDateTime fechaExp = expiracion.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();

            tokenBlacklistService.revocar(token, email, fechaExp);
        } catch (Exception e) {
            // Si el token ya expiró, igual se revoca con fecha actual
            tokenBlacklistService.revocar(token, "unknown",
                    LocalDateTime.now().plusMinutes(1));
        }
    }
}
