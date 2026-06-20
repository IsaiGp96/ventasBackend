package com.app.backend.proveedor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProveedorRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String telefono;
    private String correo;
    private String direccion;
    private String rfc;
    private String prefijo;
}