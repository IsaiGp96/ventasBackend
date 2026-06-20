package com.app.backend.inventario.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EntradaInventarioRequest {

    @NotNull(message = "La orden de compra es obligatoria")
    private Integer idOrdenCompra;

    private String nota;
}