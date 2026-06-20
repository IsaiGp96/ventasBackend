package com.app.backend.cliente.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El tipo es obligatorio")
    private String tipo; // general | mayoreo

    private String telefono;
    private String correo;
    private String direccion;
    private String rfc;

    @DecimalMin(value = "0", message = "El límite de crédito no puede ser negativo")
    private BigDecimal limiteCredito;
}
