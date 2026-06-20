package com.app.backend.venta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.math.BigDecimal;

@Data
public class VentaRequest {

    private Integer idCliente; // null = público general

    @NotBlank(message = "El método de pago es obligatorio")
    private String metodoPago; // efectivo|transferencia|tarjeta|credito

    private BigDecimal descuento; // porcentaje 0-100
    private String notas;

    @NotEmpty(message = "Debe agregar al menos un producto")
    @Valid
    private List<DetalleVentaRequest> detalles;

}