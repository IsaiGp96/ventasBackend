package com.app.backend.cliente.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ClienteResponse {
    private Integer id;
    private String nombre;
    private String tipo;
    private String telefono;
    private String correo;
    private String direccion;
    private String rfc;
    private BigDecimal limiteCredito;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private Boolean tieneCredito; // limiteCredito > 0
}