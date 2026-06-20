package com.app.backend.security;

// import com.app.backend.security.service.RateLimitService;
import com.app.backend.security.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    // private final RateLimitService rateLimitService; // ← nuevo
    // private static final String RATE_LIMIT_CHECKED = "rateLimitChecked";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        System.out.println(">>> JwtAuthFilter interceptando: " + path);

        // ─── 1. Rate limiting — primera línea de defensa ──────────────────────
        // Aplica a todas las rutas API antes de cualquier procesamiento
        // if (path.startsWith("/api/") && request.getAttribute(RATE_LIMIT_CHECKED) == null) {
        //     request.setAttribute(RATE_LIMIT_CHECKED, true); // marcar como procesado

        //     String ip = obtenerIp(request);
        //     String endpoint = normalizarEndpoint(path);

        //     if (!rateLimitService.permitir(ip, endpoint)) {
        //         response.setStatus(429);
        //         response.setContentType("application/json");
        //         response.getWriter().write(
        //                 "{\"message\":\"Demasiadas solicitudes. Intenta en un momento.\"}");
        //         return;
        //     }
        // }

        // ─── 2. Rutas públicas pasan directo ──────────────────────────────────
        if (path.contains("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ─── 3. Verificar header Authorization ────────────────────────────────
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        // ─── 4. Blacklist — antes de validar el token ─────────────────────────
        // Fix: en la versión anterior esto estaba al final, permitiendo
        // que un token revocado se autenticara antes de ser rechazado
        if (tokenBlacklistService.estaRevocado(jwt)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Token revocado\"}");
            return;
        }

        // ─── 5. Extraer usuario del token ─────────────────────────────────────
        final String userEmail;
        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            System.out.println("ERROR en username del token: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Token expirado o inválido\"}");
            return;
        }

        // ─── 6. Autenticar en el contexto de seguridad ────────────────────────
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Token inválido\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String obtenerIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizarEndpoint(String uri) {
        // Agrupa /api/productos/1, /api/productos/2, etc. en un solo contador
        return uri.replaceAll("/\\d+", "/{id}");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }
}