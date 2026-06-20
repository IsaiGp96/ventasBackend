package com.app.backend.compra.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class DetalleOrdenResponse {
    private Integer id;
    private Integer idVariante;
    private String sku;
    private String producto;
    private Integer cantidad;
    private BigDecimal costoUnitario;
    private BigDecimal subtotal;
    private Map<String, String> atributos;

}
