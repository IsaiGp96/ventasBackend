package com.app.backend.security.filter;

import com.app.backend.security.service.IdempotenciaService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(2)
@RequiredArgsConstructor
public class IdempotenciaFilter implements Filter {

    private final IdempotenciaService idempotenciaService;

    // Solo aplica a métodos que modifican estado
    private static final java.util.Set<String> METODOS = java.util.Set.of("POST", "PUT", "PATCH");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String metodo = request.getMethod();
        String uri = request.getRequestURI();

        // Solo aplica a rutas API con métodos de escritura
        if (!uri.startsWith("/api/") || !METODOS.contains(metodo)
                || uri.contains("/api/auth/")) {
            chain.doFilter(req, res);
            return;
        }

        String key = request.getHeader("Idempotency-Key");

        // Si no viene la clave, se permite el request pero sin protección
        if (key == null || key.isBlank()) {
            chain.doFilter(req, res);
            return;
        }

        // Buscar respuesta previa
        var previo = idempotenciaService.buscar(key);
        if (previo.isPresent()) {
            // Devolver la respuesta cacheada exacta
            var registro = previo.get();
            response.setStatus(registro.getResponseStatus());
            response.setContentType("application/json");
            response.getWriter().write(
                    registro.getResponseBody() != null ? registro.getResponseBody() : "{}");
            return;
        }

        // Request nuevo — usar wrapper para capturar la respuesta
        CachedResponseWrapper wrapper = new CachedResponseWrapper(response);
        chain.doFilter(req, wrapper);

        // Guardar respuesta solo si fue exitosa (2xx)
        int status = wrapper.getStatus();
        if (status >= 200 && status < 300) {
            String body = wrapper.getCapturedBody();
            idempotenciaService.guardar(key, uri, status, body);
        }

        // Escribir la respuesta real al cliente
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(wrapper.getCapturedBody());
    }

    // Wrapper para capturar el body de la respuesta
    private static class CachedResponseWrapper extends HttpServletResponseWrapper {
        private final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        private final java.io.PrintWriter writer;

        public CachedResponseWrapper(HttpServletResponse response) throws IOException {
            super(response);
            this.writer = new java.io.PrintWriter(buffer);
        }

        @Override
        public java.io.PrintWriter getWriter() {
            return writer;
        }

        public String getCapturedBody() {
            writer.flush();
            return buffer.toString();
        }
    }
}
