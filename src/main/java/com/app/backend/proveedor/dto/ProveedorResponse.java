package com.app.backend.proveedor.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProveedorResponse {
    private Integer id;
    private String nombre;
    private String telefono;
    private String correo;
    private String direccion;
    private String rfc;
    private String prefijo;
}