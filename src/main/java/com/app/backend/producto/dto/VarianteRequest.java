package com.app.backend.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.Map;

@Data
public class VarianteRequest {

    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    private String codigoBarras;

    @NotEmpty(message = "Debe tener al menos un atributo")
    private Map<String, String> atributos; // {"genero":"Hombre","talla":"M","color":"Negro"}
                                           // {"tipo":"Básico","talla":"Unitalla"}
}