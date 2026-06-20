package com.app.backend.producto.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El tipo de producto es obligatorio")
    private Integer idTipoProducto;

    private Integer idProveedor;

    private BigDecimal precioCompra;

    private BigDecimal precioVenta;

    private BigDecimal pctComision;

    private Integer idCategoria;

}