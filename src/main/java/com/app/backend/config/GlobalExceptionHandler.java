package com.app.backend.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        // Errores de validación (@Valid)
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidation(
                        MethodArgumentNotValidException ex) {

                String mensaje = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(e -> e.getDefaultMessage())
                                .findFirst()
                                .orElse("Error de validación");

                return ResponseEntity
                                .badRequest()
                                .body(Map.of("message", mensaje));
        }

        // Errores de negocio (IllegalArgumentException)
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, String>> handleIllegalArgument(
                        IllegalArgumentException ex) {

                return ResponseEntity
                                .badRequest()
                                .body(Map.of("message", ex.getMessage()));
        }

        // Cualquier otro error — no expone detalles internos
        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {

                // Log interno para debugging (puedes usar un logger real aquí)
                System.err.println("[ERROR] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("message", "Ocurrió un error interno. Intenta de nuevo."));
        }

        // Manejo de contraseña incorrecta.
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("message", "Credenciales incorrectas"));
        }
}