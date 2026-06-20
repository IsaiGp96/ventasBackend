package com.app.backend.producto.dto;

public interface CatalogoProjection {
    Integer getIdVariante();

    String getSku();

    String getCodigoBarras();

    Integer getIdProducto();

    String getNombre();

    String getDescripcion();

    String getTipo();

    // String getGenero();

    // String getTalla();

    // String getColor();

    String getCodigoHex();

    java.math.BigDecimal getPrecioCompra();

    java.math.BigDecimal getPrecioVenta();

    Integer getStock();

    String getImagenUrl();

    String getAtributos(); // JSON string de variante_atributo
}