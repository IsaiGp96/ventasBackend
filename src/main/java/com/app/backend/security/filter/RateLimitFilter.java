package com.app.backend.security.filter;

import com.app.backend.security.service.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1) // corre antes que los filtros de Spring Security
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;
    private static final String RATE_LIMIT_CHECKED = "rateLimitChecked";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // String ip = obtenerIp(request);
        // String endpoint = normalizarEndpoint(request.getRequestURI());

        String uri = request.getRequestURI();

        // Solo aplica a rutas de la API
        if (request.getRequestURI().startsWith("/api/") && request.getAttribute(RATE_LIMIT_CHECKED) == null) {
            request.setAttribute(RATE_LIMIT_CHECKED, true);

            String ip = obtenerIp(request);
            String endpoint = normalizarEndpoint(uri);

            if (!rateLimitService.permitir(ip, endpoint)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"message\":\"Demasiadas solicitudes. Intenta en un momento.\"}");
                return;
            }
        } else if (uri.startsWith("/api/") && request.getAttribute(RATE_LIMIT_CHECKED) != null) {
            String ip = obtenerIp(request);
            String endpoint = normalizarEndpoint(uri);

            if (!rateLimitService.permitir(ip, endpoint)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"message\":\"Demasiadas solicitudes. Intenta en un momento.\"}");
                return;
            }
        }

        chain.doFilter(req, res);
        
    }

    private String obtenerIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Normaliza /api/productos/1 → /api/productos/{id}
    // para no crear un registro por cada ID diferente
    private String normalizarEndpoint(String uri) {
        return uri.replaceAll("/\\d+", "/{id}");
    }
}
